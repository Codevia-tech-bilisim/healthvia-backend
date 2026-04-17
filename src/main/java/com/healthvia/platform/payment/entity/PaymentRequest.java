package com.healthvia.platform.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.model.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Payment request — either a sharable link (preferred, PCI-DSS-safe) or a
 * one-shot agent-assisted charge (requires explicit consent, card never
 * persisted). iyzico is the underlying gateway.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "payment_requests")
public class PaymentRequest extends BaseEntity {

    @Indexed @Field("case_id") private String caseId;
    @Indexed @Field("patient_id") private String patientId;
    @Indexed @Field("agent_id") private String agentId;

    private PaymentMethod method;
    @Indexed private PaymentStatus status;

    @Indexed(unique = true)
    @Field("link_token")
    private String linkToken;

    @Field("link_url") private String linkUrl;
    @Field("link_expires_at") private LocalDateTime linkExpiresAt;
    @Field("link_sent_at") private LocalDateTime linkSentAt;
    @Field("link_sent_via") private LinkChannel linkSentVia;

    @Field("consent_record_id")
    private String consentRecordId;

    @Field("iyzico_conversation_id") private String iyzicoConversationId;
    @Field("iyzico_payment_id") private String iyzicoPaymentId;
    @Field("iyzico_token") private String iyzicoToken;

    private BigDecimal amount;
    private String currency;
    private String description;

    @Field("related_booking_ids") private List<String> relatedBookingIds;
    @Field("related_appointment_ids") private List<String> relatedAppointmentIds;

    @Field("paid_at") private LocalDateTime paidAt;

    public boolean isExpired() {
        return linkExpiresAt != null && LocalDateTime.now().isAfter(linkExpiresAt);
    }

    public void markPaid(String iyzicoPaymentId) {
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.iyzicoPaymentId = iyzicoPaymentId;
    }

    public enum PaymentMethod { LINK, AGENT_ASSISTED, BANK_TRANSFER }
    public enum PaymentStatus { PENDING, LINK_SENT, PAID, FAILED, EXPIRED, REFUNDED }
    public enum LinkChannel { SMS, WHATSAPP, EMAIL }
}
