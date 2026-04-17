package com.healthvia.platform.consent.dto;

import java.util.Map;

import com.healthvia.platform.consent.entity.Consent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestDto {
    @NotBlank private String patientId;
    @NotBlank private String caseId;
    @NotNull  private Consent.ConsentType type;
    @NotNull  private Consent.ConsentChannel channel;
    @NotBlank private String phoneNumber;
    @NotBlank private String scopeDescription;
    private Map<String, Object> scopeDetails;
}
