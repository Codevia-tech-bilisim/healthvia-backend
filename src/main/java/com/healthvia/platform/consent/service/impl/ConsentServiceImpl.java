package com.healthvia.platform.consent.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.consent.dto.ConsentRequestDto;
import com.healthvia.platform.consent.entity.Consent;
import com.healthvia.platform.consent.repository.ConsentRepository;
import com.healthvia.platform.consent.service.ConsentService;
import com.healthvia.platform.consent.sms.SmsProvider;
import com.healthvia.platform.patientcase.service.PatientCaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsentServiceImpl implements ConsentService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;

    private final ConsentRepository consentRepository;
    private final SmsProvider smsProvider;
    private final PatientCaseService caseService;

    @Override
    public Consent requestConsent(
            ConsentRequestDto req,
            String requestedByAgentId,
            String ipAddress,
            String userAgent) {

        String otp = generateOtp();
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());

        Consent consent = Consent.builder()
            .patientId(req.getPatientId())
            .caseId(req.getCaseId())
            .requestedByAgentId(requestedByAgentId)
            .type(req.getType())
            .status(Consent.ConsentStatus.PENDING)
            .channel(req.getChannel())
            .phoneNumber(req.getPhoneNumber())
            .otpCodeHash(hash)
            .attemptCount(0)
            .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
            .scopeDescription(req.getScopeDescription())
            .scopeDetails(req.getScopeDetails())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        Consent saved = consentRepository.save(consent);

        String message = String.format(
            "HealthVia onay kodu: %s. Bu kod %s için geçerli. Kodu asla başkasıyla paylaşmayın.",
            otp, req.getScopeDescription());

        try {
            if (req.getChannel() == Consent.ConsentChannel.WHATSAPP) {
                smsProvider.sendWhatsApp(req.getPhoneNumber(), message);
            } else {
                smsProvider.send(req.getPhoneNumber(), message);
            }
        } catch (Exception e) {
            log.error("OTP gönderimi başarısız: {}", e.getMessage(), e);
            throw new IllegalStateException("OTP gönderilemedi: " + e.getMessage());
        }

        log.info("Consent oluşturuldu: {} (type={}, channel={})", saved.getId(), saved.getType(), saved.getChannel());
        return saved;
    }

    @Override
    public Consent verifyConsent(String consentId, String otp) {
        Consent consent = findByIdOrThrow(consentId);

        if (consent.getStatus() != Consent.ConsentStatus.PENDING) {
            throw new IllegalStateException("Bu onay zaten " + consent.getStatus() + " durumunda");
        }

        if (consent.isExpired()) {
            consent.markExpired();
            consentRepository.save(consent);
            throw new IllegalStateException("Doğrulama süresi doldu");
        }

        if (!consent.canAttemptVerification()) {
            consent.markRejected();
            consentRepository.save(consent);
            throw new IllegalStateException("Deneme hakkı doldu");
        }

        consent.recordAttempt();

        boolean match = BCrypt.checkpw(otp, consent.getOtpCodeHash());
        if (!match) {
            consentRepository.save(consent);
            throw new IllegalArgumentException("Doğrulama kodu yanlış");
        }

        consent.markApproved();
        Consent approved = consentRepository.save(consent);

        // Bag consent into the case
        try {
            caseService.addConsent(approved.getCaseId(), approved.getId());
        } catch (Exception e) {
            log.warn("Consent case'e eklenemedi: {}", e.getMessage());
        }

        log.info("Consent onaylandı: {}", approved.getId());
        return approved;
    }

    @Override
    public Consent findByIdOrThrow(String id) {
        return consentRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Consent", "id", id));
    }

    @Override
    public List<Consent> findByCase(String caseId) {
        return consentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId);
    }

    @Override
    public List<Consent> findByPatient(String patientId) {
        return consentRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId);
    }

    @Override
    public void assertApproved(String consentId) {
        if (consentId == null || consentId.isBlank()) {
            throw new IllegalStateException("Bu işlem için hasta onayı zorunludur");
        }
        Consent c = findByIdOrThrow(consentId);
        if (c.getStatus() != Consent.ConsentStatus.APPROVED) {
            throw new IllegalStateException(
                "Onay durumu: " + c.getStatus() + " — işlem için onaylı consent gereklidir");
        }
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }
}
