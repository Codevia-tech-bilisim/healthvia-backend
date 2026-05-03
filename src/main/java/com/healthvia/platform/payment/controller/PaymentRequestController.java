package com.healthvia.platform.payment.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.payment.entity.PaymentRequest;
import com.healthvia.platform.payment.service.PaymentRequestService;
import com.healthvia.platform.payment.service.impl.PaymentRequestServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentRequestController {

    private final PaymentRequestService service;
    private final PaymentRequestServiceImpl serviceImpl;

    @PostMapping("/link")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
    public ApiResponse<PaymentRequest> createLink(@RequestBody CreateLinkRequest req) {
        PaymentRequest pr = service.createLink(
            req.caseId(), req.amount(), req.currency(), req.description(), req.channel());
        return ApiResponse.success(pr, "Ödeme linki oluşturuldu");
    }

    @PostMapping("/agent-assisted")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
    public ApiResponse<PaymentRequest> chargeAgentAssisted(@RequestBody AgentChargeRequest req) {
        PaymentRequest pr = service.chargeAgentAssisted(
            req.caseId(), req.amount(), req.currency(), req.description(), req.consentId(),
            new PaymentRequestService.AgentAssistedCard(
                req.cardData().holderName(),
                req.cardData().number(),
                req.cardData().expireMonth(),
                req.cardData().expireYear(),
                req.cardData().cvc()));
        return ApiResponse.success(pr, "Ödeme alındı");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT', 'CEO')")
    public ApiResponse<List<PaymentRequest>> list(
            @RequestParam(required = false) String caseId,
            @RequestParam(required = false) String agentId) {
        if (caseId != null) return ApiResponse.success(service.findByCase(caseId));
        if (agentId != null) return ApiResponse.success(service.findByAgent(agentId));
        return ApiResponse.success(List.of());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT', 'CEO')")
    public ApiResponse<PaymentRequest> getOne(@PathVariable String id) {
        return ApiResponse.success(service.findByIdOrThrow(id));
    }

    /** Public endpoint for hasta (patient) to fetch their payment details by link token. */
    @GetMapping("/public/by-token/{token}")
    public ApiResponse<PaymentRequest> getByLinkToken(@PathVariable String token) {
        return service.findByLinkToken(token)
            .map(ApiResponse::success)
            .orElseGet(() -> ApiResponse.error("Ödeme linki bulunamadı veya süresi dolmuş"));
    }

    /**
     * iyzico Checkout Form callback. Patient pays on iyzico's hosted page;
     * iyzico POSTs back to this URL with `token=<iyzicoToken>` plus our own
     * `?token=<ourToken>` query param. We retrieve the final payment status
     * from iyzico via CheckoutForm.retrieve() and mark the PaymentRequest as
     * PAID, which in turn registers it on the case. Idempotent.
     */
    @PostMapping(value = "/public/iyzico-callback",
        consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> iyzicoCallback(
            @RequestParam("token") String ourToken,
            @RequestParam(value = "token", required = false) String iyzicoToken,
            @RequestBody(required = false) MultiValueMap<String, String> formBody) {
        String iyzToken = iyzicoToken;
        if ((iyzToken == null || iyzToken.isBlank()) && formBody != null) {
            iyzToken = formBody.getFirst("token");
        }
        if (iyzToken == null || iyzToken.isBlank()) {
            return ResponseEntity.badRequest().body(htmlMessage("Eksik token", false));
        }
        try {
            PaymentRequest pr = serviceImpl.confirmCheckoutForm(ourToken, iyzToken);
            boolean paid = pr.getStatus() == PaymentRequest.PaymentStatus.PAID;
            return ResponseEntity.ok(htmlMessage(
                paid ? "Ödemeniz alındı. HealthVia ekibinden randevu detayları gelecek."
                     : "Ödeme tamamlanamadı. Lütfen agent ile tekrar görüşün.",
                paid));
        } catch (Exception e) {
            return ResponseEntity.ok(htmlMessage("Ödeme doğrulanamadı: " + e.getMessage(), false));
        }
    }

    private static String htmlMessage(String msg, boolean ok) {
        String color = ok ? "#10b981" : "#ef4444";
        return "<!doctype html><html lang=tr><meta charset=utf-8>"
            + "<title>HealthVia · Ödeme</title>"
            + "<style>body{font-family:Inter,system-ui,sans-serif;background:#f8f9fa;"
            + "display:grid;place-items:center;min-height:100vh;margin:0}"
            + ".c{background:#fff;padding:48px 56px;border-radius:24px;"
            + "box-shadow:0 20px 60px rgba(15,23,42,.08);max-width:480px;text-align:center}"
            + ".d{width:64px;height:64px;border-radius:50%;background:" + color + ";"
            + "color:#fff;font-size:32px;display:grid;place-items:center;margin:0 auto 16px}"
            + "h1{color:#1a2b4b;margin:0 0 8px;font-size:22px}p{color:#64748b;margin:0}</style>"
            + "<div class=c><div class=d>" + (ok ? "✓" : "✕") + "</div>"
            + "<h1>HealthVia</h1><p>" + msg + "</p></div>";
    }

    /**
     * iyzico webhook — server-to-server payment notifications (separate from
     * Checkout Form callback). Currently unused in the sandbox flow but kept
     * for future direct-API or 3DS integrations.
     */
    @PostMapping("/public/webhook/iyzico")
    public ApiResponse<Void> iyzicoWebhook(@RequestBody IyzicoWebhookPayload payload) {
        if (payload == null || payload.token() == null) {
            return ApiResponse.error("Invalid webhook payload");
        }
        return service.findByLinkToken(payload.token())
            .map(pr -> {
                if (!"SUCCESS".equalsIgnoreCase(payload.status())) {
                    return ApiResponse.<Void>success("Ignored non-success webhook");
                }
                service.markPaid(pr.getId(), payload.paymentId());
                return ApiResponse.<Void>success("Ödeme başarıyla işlendi");
            })
            .orElseGet(() -> ApiResponse.error("Token eşleşmedi"));
    }

    public record CreateLinkRequest(
        String caseId,
        BigDecimal amount,
        String currency,
        String description,
        PaymentRequest.LinkChannel channel) {}

    public record AgentChargeRequest(
        String caseId,
        BigDecimal amount,
        String currency,
        String description,
        String consentId,
        CardPayload cardData) {}

    public record CardPayload(
        String holderName,
        String number,
        String expireMonth,
        String expireYear,
        String cvc) {}

    public record IyzicoWebhookPayload(
        String token,
        String paymentId,
        String status,
        String currency) {}
}
