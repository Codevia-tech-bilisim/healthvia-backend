package com.healthvia.platform.payment.service.impl;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.consent.service.ConsentService;
import com.healthvia.platform.patientcase.service.PatientCaseService;
import com.healthvia.platform.payment.entity.PaymentRequest;
import com.healthvia.platform.payment.repository.PaymentRequestRepository;
import com.healthvia.platform.payment.service.PaymentRequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentRequestServiceImpl implements PaymentRequestService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 24;
    private static final int LINK_TTL_HOURS = 24;

    @Value("${payment.public-base-url:https://pay.healthviatech.website}")
    private String publicBaseUrl;

    private final PaymentRequestRepository repository;
    private final PatientCaseService caseService;
    private final ConsentService consentService;

    @Override
    public PaymentRequest createLink(
            String caseId,
            BigDecimal amount,
            String currency,
            String description,
            PaymentRequest.LinkChannel channel) {

        caseService.findByIdOrThrow(caseId);
        String token = generateToken();

        PaymentRequest pr = PaymentRequest.builder()
            .caseId(caseId)
            .patientId(caseService.findByIdOrThrow(caseId).getPatientId())
            .agentId(SecurityUtils.getCurrentUserIdOrNull())
            .method(PaymentRequest.PaymentMethod.LINK)
            .status(PaymentRequest.PaymentStatus.LINK_SENT)
            .linkToken(token)
            .linkUrl(publicBaseUrl + "/p/" + token)
            .linkExpiresAt(LocalDateTime.now().plusHours(LINK_TTL_HOURS))
            .linkSentAt(LocalDateTime.now())
            .linkSentVia(channel)
            .amount(amount)
            .currency(currency)
            .description(description)
            .build();

        PaymentRequest saved = repository.save(pr);
        caseService.addPaymentRequest(caseId, saved.getId(), amount);
        log.info("Ödeme linki oluşturuldu: {} ({} {}) → {}", saved.getId(), amount, currency, channel);
        return saved;
    }

    @Override
    public PaymentRequest chargeAgentAssisted(
            String caseId,
            BigDecimal amount,
            String currency,
            String description,
            String consentId,
            AgentAssistedCard card) {

        consentService.assertApproved(consentId);
        caseService.findByIdOrThrow(caseId);

        // 🚨 CARD HOLDER / NUMBER / CVC are NEVER persisted or logged.
        // In production, forward to iyzico.charge(...) here and capture response.
        String iyzicoPaymentId = "iyz_pending_" + generateToken();

        PaymentRequest pr = PaymentRequest.builder()
            .caseId(caseId)
            .patientId(caseService.findByIdOrThrow(caseId).getPatientId())
            .agentId(SecurityUtils.getCurrentUserIdOrNull())
            .method(PaymentRequest.PaymentMethod.AGENT_ASSISTED)
            .status(PaymentRequest.PaymentStatus.PENDING)
            .consentRecordId(consentId)
            .iyzicoPaymentId(iyzicoPaymentId)
            .amount(amount)
            .currency(currency)
            .description(description)
            .build();

        PaymentRequest saved = repository.save(pr);

        // Pretend iyzico returned success synchronously for this MVP path.
        saved.markPaid(iyzicoPaymentId);
        saved = repository.save(saved);

        caseService.addPaymentRequest(caseId, saved.getId(), amount);
        caseService.registerPayment(caseId, amount);

        log.info("Agent-assisted ödeme alındı: {} ({} {})", saved.getId(), amount, currency);
        return saved;
    }

    @Override
    public Optional<PaymentRequest> findByLinkToken(String token) {
        return repository.findByLinkTokenAndDeletedFalse(token);
    }

    @Override
    public PaymentRequest findByIdOrThrow(String id) {
        return repository.findById(id)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("PaymentRequest", "id", id));
    }

    @Override
    public List<PaymentRequest> findByCase(String caseId) {
        return repository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId);
    }

    @Override
    public List<PaymentRequest> findByAgent(String agentId) {
        return repository.findByAgentIdAndDeletedFalseOrderByCreatedAtDesc(agentId);
    }

    @Override
    public PaymentRequest markPaid(String id, String iyzicoPaymentId) {
        PaymentRequest p = findByIdOrThrow(id);
        p.markPaid(iyzicoPaymentId);
        PaymentRequest saved = repository.save(p);
        caseService.registerPayment(p.getCaseId(), p.getAmount());
        return saved;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
