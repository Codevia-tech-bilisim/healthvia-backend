package com.healthvia.platform.consent.service;

import java.util.List;

import com.healthvia.platform.consent.dto.ConsentRequestDto;
import com.healthvia.platform.consent.entity.Consent;

public interface ConsentService {

    Consent requestConsent(ConsentRequestDto request, String requestedByAgentId, String ipAddress, String userAgent);

    Consent verifyConsent(String consentId, String otp);

    Consent findByIdOrThrow(String id);

    List<Consent> findByCase(String caseId);

    List<Consent> findByPatient(String patientId);

    /** Throws if consent is missing, not approved, or expired. Used by AOBO services as a guard. */
    void assertApproved(String consentId);
}
