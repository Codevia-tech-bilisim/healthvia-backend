package com.healthvia.platform.consent.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.healthvia.platform.consent.entity.Consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentDto {
    private String id;
    private String patientId;
    private String caseId;
    private String requestedByAgentId;
    private Consent.ConsentType type;
    private Consent.ConsentStatus status;
    private Consent.ConsentChannel channel;
    private String phoneNumber;
    private Integer attemptCount;
    private LocalDateTime expiresAt;
    private String scopeDescription;
    private Map<String, Object> scopeDetails;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    public static ConsentDto fromEntity(Consent c) {
        if (c == null) return null;
        return ConsentDto.builder()
            .id(c.getId())
            .patientId(c.getPatientId())
            .caseId(c.getCaseId())
            .requestedByAgentId(c.getRequestedByAgentId())
            .type(c.getType())
            .status(c.getStatus())
            .channel(c.getChannel())
            .phoneNumber(maskPhone(c.getPhoneNumber()))
            .attemptCount(c.getAttemptCount())
            .expiresAt(c.getExpiresAt())
            .scopeDescription(c.getScopeDescription())
            .scopeDetails(c.getScopeDetails())
            .approvedAt(c.getApprovedAt())
            .createdAt(c.getCreatedAt())
            .build();
    }

    private static String maskPhone(String p) {
        if (p == null || p.length() < 6) return p;
        int keep = 3;
        return p.substring(0, p.length() - keep).replaceAll("\\d", "•") + p.substring(p.length() - keep);
    }
}
