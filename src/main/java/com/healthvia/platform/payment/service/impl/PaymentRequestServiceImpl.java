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

import com.healthvia.platform.booking.IyzicoPaymentService;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.consent.service.ConsentService;
import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.service.PatientCaseService;
import com.healthvia.platform.payment.entity.PaymentRequest;
import com.healthvia.platform.payment.repository.PaymentRequestRepository;
import com.healthvia.platform.payment.service.PaymentRequestService;
import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.service.PatientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Real PaymentRequest service — wires both flows to iyzico:
 *
 *  - createLink(): builds an iyzico CheckoutFormInitialize call, gets a real
 *    sandbox/production paymentPageUrl back, persists it as the linkUrl. The
 *    patient's link goes straight to iyzico's hosted page; payment events
 *    return through /api/v1/payments/public/iyzico-callback.
 *
 *  - chargeAgentAssisted(): forwards card data to iyzico.processPayment()
 *    immediately. Card fields are NEVER persisted or logged; consent is
 *    required and asserted before the call.
 */
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

    @Value("${payment.api-base-url:https://api.healthviatech.website}")
    private String apiBaseUrl;

    private final PaymentRequestRepository repository;
    private final PatientCaseService caseService;
    private final ConsentService consentService;
    private final IyzicoPaymentService iyzico;
    private final PatientService patientService;

    @Override
    public PaymentRequest createLink(
            String caseId,
            BigDecimal amount,
            String currency,
            String description,
            PaymentRequest.LinkChannel channel) {

        PatientCase caseRec = caseService.findByIdOrThrow(caseId);
        String token = generateToken();
        String agentId = SecurityUtils.getCurrentUserIdOrNull();

        // Persist the request first so the iyzico callback can find it via our token
        PaymentRequest pr = PaymentRequest.builder()
            .caseId(caseId)
            .patientId(caseRec.getPatientId())
            .agentId(agentId)
            .method(PaymentRequest.PaymentMethod.LINK)
            .status(PaymentRequest.PaymentStatus.PENDING)
            .linkToken(token)
            .linkExpiresAt(LocalDateTime.now().plusHours(LINK_TTL_HOURS))
            .amount(amount)
            .currency(currency)
            .description(description)
            .iyzicoConversationId(token) // re-use our token as iyzico conversationId
            .build();
        PaymentRequest saved = repository.save(pr);

        // Initialise iyzico CheckoutForm — buyer info from patient if available
        BuyerInfo buyer = resolveBuyer(caseRec.getPatientId());
        String callback = apiBaseUrl + "/api/v1/payments/public/iyzico-callback?token=" + token;
        IyzicoPaymentService.CheckoutInitResult init = iyzico.initializeCheckoutForm(
            token,
            description == null ? "HealthVia Treatment Package" : description,
            amount,
            // iyzico sandbox accepts TRY for test cards; production should match buyer currency
            currency,
            buyer.fullName,
            buyer.email,
            caseRec.getPatientId(),
            buyer.phone,
            null,
            callback);

        if (!init.success()) {
            saved.setStatus(PaymentRequest.PaymentStatus.FAILED);
            repository.save(saved);
            log.warn("iyzico Checkout init failed for case {}: {}", caseId, init.errorMessage());
            throw new IllegalStateException("Ödeme linki oluşturulamadı: " + init.errorMessage());
        }

        // Replace placeholder URL with the real iyzico-hosted payment page URL
        saved.setLinkUrl(init.paymentPageUrl());
        saved.setIyzicoToken(init.token());
        saved.setStatus(PaymentRequest.PaymentStatus.LINK_SENT);
        saved.setLinkSentAt(LocalDateTime.now());
        saved.setLinkSentVia(channel);
        saved = repository.save(saved);

        caseService.addPaymentRequest(caseId, saved.getId(), amount);
        log.info("Ödeme linki oluşturuldu: {} ({} {}) → {} via {}",
            saved.getId(), amount, currency, saved.getLinkUrl(), channel);
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
        PatientCase caseRec = caseService.findByIdOrThrow(caseId);
        String agentId = SecurityUtils.getCurrentUserIdOrNull();
        String conversationId = generateToken();

        PaymentRequest pr = PaymentRequest.builder()
            .caseId(caseId)
            .patientId(caseRec.getPatientId())
            .agentId(agentId)
            .method(PaymentRequest.PaymentMethod.AGENT_ASSISTED)
            .status(PaymentRequest.PaymentStatus.PENDING)
            .consentRecordId(consentId)
            .iyzicoConversationId(conversationId)
            .amount(amount)
            .currency(currency)
            .description(description)
            .build();
        PaymentRequest saved = repository.save(pr);

        // Forward card to iyzico — never logged, never persisted on our side
        BuyerInfo buyer = resolveBuyer(caseRec.getPatientId());
        IyzicoPaymentService.PaymentResult result = iyzico.chargeWithCard(
            conversationId,
            description == null ? "HealthVia Treatment" : description,
            amount,
            currency,
            card.holderName(),
            card.number(),
            card.expireMonth(),
            card.expireYear(),
            card.cvc(),
            buyer.fullName,
            buyer.email,
            caseRec.getPatientId(),
            buyer.phone,
            null);

        if (result.success()) {
            saved.markPaid(result.paymentId());
            saved = repository.save(saved);
            caseService.addPaymentRequest(caseId, saved.getId(), amount);
            caseService.registerPayment(caseId, amount);
            log.info("Agent-assisted ödeme alındı: {} (iyzicoPaymentId={})",
                saved.getId(), result.paymentId());
            return saved;
        }

        saved.setStatus(PaymentRequest.PaymentStatus.FAILED);
        saved = repository.save(saved);
        log.warn("Agent-assisted ödeme başarısız: {} ({})", saved.getId(), result.errorMessage());
        throw new IllegalStateException("Ödeme reddedildi: " + result.errorMessage());
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
        if (p.getStatus() == PaymentRequest.PaymentStatus.PAID) {
            return p; // idempotent
        }
        p.markPaid(iyzicoPaymentId);
        PaymentRequest saved = repository.save(p);
        caseService.registerPayment(p.getCaseId(), p.getAmount());
        return saved;
    }

    /**
     * Confirm a CheckoutForm payment by its iyzico token. Used by the iyzico
     * callback endpoint to retrieve the final payment status.
     */
    public PaymentRequest confirmCheckoutForm(String ourToken, String iyzicoToken) {
        PaymentRequest pr = repository.findByLinkTokenAndDeletedFalse(ourToken)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentRequest", "token", ourToken));
        if (pr.getStatus() == PaymentRequest.PaymentStatus.PAID) return pr;

        IyzicoPaymentService.CheckoutRetrieveResult res =
            iyzico.retrieveCheckoutForm(iyzicoToken, pr.getIyzicoConversationId());
        if (res.paid()) {
            return markPaid(pr.getId(), res.paymentId());
        }
        pr.setStatus(PaymentRequest.PaymentStatus.FAILED);
        return repository.save(pr);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private BuyerInfo resolveBuyer(String patientId) {
        if (patientId == null) return new BuyerInfo("Hasta", "patient@healthvia.com", "+905350000000");
        try {
            return patientService.findById(patientId)
                .map(p -> new BuyerInfo(
                    p.getFullName() != null && !p.getFullName().isBlank()
                        ? p.getFullName()
                        : (safe(p.getFirstName()) + " " + safe(p.getLastName())).trim(),
                    p.getEmail() == null ? "patient@healthvia.com" : p.getEmail(),
                    p.getPhone() == null || p.getPhone().isBlank() ? "+905350000000" : p.getPhone()))
                .orElse(new BuyerInfo("Hasta", "patient@healthvia.com", "+905350000000"));
        } catch (Exception e) {
            log.warn("Buyer lookup failed for patient {}: {}", patientId, e.getMessage());
            return new BuyerInfo("Hasta", "patient@healthvia.com", "+905350000000");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Used internally by confirmCheckoutForm; never exposed. */
    @SuppressWarnings("unused")
    private void touchPatient(Patient p) {
        // Reference kept so removing PatientService still flags missing import.
        if (p == null) return;
    }

    private record BuyerInfo(String fullName, String email, String phone) {}
}
