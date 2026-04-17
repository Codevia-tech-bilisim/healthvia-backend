package com.healthvia.platform.patientcase.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.entity.PatientCase.CaseStatus;

public interface PatientCaseService {

    PatientCase createFromLead(String leadId);

    Optional<PatientCase> findById(String id);

    PatientCase findByIdOrThrow(String id);

    Page<PatientCase> findAll(Pageable pageable);

    Page<PatientCase> findByStatus(List<CaseStatus> statuses, Pageable pageable);

    Page<PatientCase> findByAgent(String agentId, Pageable pageable);

    Page<PatientCase> search(String keyword, Pageable pageable);

    PatientCase changeStatus(String id, CaseStatus status);

    PatientCase addAppointment(String caseId, String appointmentId);

    PatientCase addFlightBooking(String caseId, String flightBookingId, BigDecimal amount);

    PatientCase addHotelBooking(String caseId, String hotelBookingId, BigDecimal amount);

    PatientCase addPaymentRequest(String caseId, String paymentRequestId, BigDecimal totalImpact);

    PatientCase registerPayment(String caseId, BigDecimal amount);

    PatientCase addConsent(String caseId, String consentId);
}
