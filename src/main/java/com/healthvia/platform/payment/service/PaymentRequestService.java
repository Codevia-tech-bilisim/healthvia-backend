package com.healthvia.platform.payment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.healthvia.platform.payment.entity.PaymentRequest;

public interface PaymentRequestService {

    /** Generate a shareable iyzico payment link for patient self-checkout. */
    PaymentRequest createLink(
        String caseId,
        BigDecimal amount,
        String currency,
        String description,
        PaymentRequest.LinkChannel channel);

    /**
     * Agent-assisted card charge. Card fields are forwarded to iyzico and
     * NEVER persisted. Requires approved consent.
     */
    PaymentRequest chargeAgentAssisted(
        String caseId,
        BigDecimal amount,
        String currency,
        String description,
        String consentId,
        AgentAssistedCard card);

    Optional<PaymentRequest> findByLinkToken(String token);

    PaymentRequest findByIdOrThrow(String id);

    List<PaymentRequest> findByCase(String caseId);

    List<PaymentRequest> findByAgent(String agentId);

    PaymentRequest markPaid(String id, String iyzicoPaymentId);

    record AgentAssistedCard(
        String holderName,
        String number,
        String expireMonth,
        String expireYear,
        String cvc) {}
}
