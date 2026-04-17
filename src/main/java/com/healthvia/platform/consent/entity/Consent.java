package com.healthvia.platform.consent.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
 * KVKK/GDPR-required patient consent record before any Agent-on-Behalf-Of
 * (AOBO) action. An OTP is sent via SMS or WhatsApp, the patient reads it
 * back to the agent, the agent enters it. Approved consents are required by
 * the booking/payment services before creating records.
 *
 * Raw OTP is NEVER stored — only a BCrypt hash. Immutable after approval.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "consents")
public class Consent extends BaseEntity {

    @Indexed
    @Field("patient_id")
    private String patientId;

    @Indexed
    @Field("case_id")
    private String caseId;

    @Indexed
    @Field("requested_by_agent_id")
    private String requestedByAgentId;

    private ConsentType type;

    @Indexed
    private ConsentStatus status;

    private ConsentChannel channel;

    @Field("phone_number")
    private String phoneNumber;

    @Field("otp_code_hash")
    private String otpCodeHash;

    @Field("attempt_count")
    private Integer attemptCount;

    @Field("expires_at")
    private LocalDateTime expiresAt;

    @Field("scope_description")
    private String scopeDescription;

    @Field("scope_details")
    private Map<String, Object> scopeDetails;

    @Field("approved_at")
    private LocalDateTime approvedAt;

    @Field("rejected_at")
    private LocalDateTime rejectedAt;

    @Field("ip_address")
    private String ipAddress;

    @Field("user_agent")
    private String userAgent;

    public void recordAttempt() {
        this.attemptCount = (this.attemptCount == null ? 0 : this.attemptCount) + 1;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canAttemptVerification() {
        return !isExpired() && (attemptCount == null || attemptCount < 3);
    }

    public void markApproved() {
        this.status = ConsentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void markRejected() {
        this.status = ConsentStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = ConsentStatus.EXPIRED;
    }

    public Map<String, Object> safeScopeDetails() {
        return scopeDetails == null ? new HashMap<>() : scopeDetails;
    }

    public enum ConsentType { BOOKING, PAYMENT, BOTH }
    public enum ConsentStatus { PENDING, APPROVED, EXPIRED, REJECTED }
    public enum ConsentChannel { SMS, WHATSAPP }
}
