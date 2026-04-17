package com.healthvia.platform.patientcase.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.service.LeadService;
import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.entity.PatientCase.CasePriority;
import com.healthvia.platform.patientcase.entity.PatientCase.CaseStatus;
import com.healthvia.platform.patientcase.repository.PatientCaseRepository;
import com.healthvia.platform.patientcase.service.PatientCaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PatientCaseServiceImpl implements PatientCaseService {

    private final PatientCaseRepository caseRepository;
    private final LeadService leadService;

    @Override
    public PatientCase createFromLead(String leadId) {
        Lead lead = leadService.findById(leadId)
            .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));

        Optional<PatientCase> existing = caseRepository.findByLeadIdAndDeletedFalse(leadId);
        if (existing.isPresent()) {
            log.info("Lead {} için case zaten mevcut: {}", leadId, existing.get().getCaseNumber());
            return existing.get();
        }

        PatientCase pc = PatientCase.builder()
            .caseNumber(generateCaseNumber())
            .patientId(lead.getConvertedPatientId() != null ? lead.getConvertedPatientId() : lead.getId())
            .leadId(lead.getId())
            .assignedAgentId(lead.getAssignedAgentId())
            .status(CaseStatus.OPEN)
            .priority(mapPriority(lead.getPriority()))
            .treatmentTypeId(lead.getTreatmentTypeId())
            .conversationId(lead.getConversationId())
            .createdOnBehalf(Boolean.FALSE)
            .totalAmount(BigDecimal.ZERO)
            .paidAmount(BigDecimal.ZERO)
            .currency("EUR")
            .build();

        PatientCase saved = caseRepository.save(pc);
        log.info("Case oluşturuldu: {} (Lead: {})", saved.getCaseNumber(), leadId);
        return saved;
    }

    @Override
    public Optional<PatientCase> findById(String id) {
        return caseRepository.findById(id).filter(c -> !c.isDeleted());
    }

    @Override
    public PatientCase findByIdOrThrow(String id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("PatientCase", "id", id));
    }

    @Override
    public Page<PatientCase> findAll(Pageable pageable) {
        return caseRepository.findAll(pageable);
    }

    @Override
    public Page<PatientCase> findByStatus(List<CaseStatus> statuses, Pageable pageable) {
        return caseRepository.findByStatusInAndDeletedFalse(statuses, pageable);
    }

    @Override
    public Page<PatientCase> findByAgent(String agentId, Pageable pageable) {
        return caseRepository.findByAssignedAgentIdAndDeletedFalse(agentId, pageable);
    }

    @Override
    public Page<PatientCase> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) return caseRepository.findAll(pageable);
        return caseRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    public PatientCase changeStatus(String id, CaseStatus status) {
        PatientCase c = findByIdOrThrow(id);
        c.setStatus(status);
        if (status == CaseStatus.COMPLETED || status == CaseStatus.CANCELLED) c.close();
        return caseRepository.save(c);
    }

    @Override
    public PatientCase addAppointment(String caseId, String appointmentId) {
        PatientCase c = findByIdOrThrow(caseId);
        c.addAppointment(appointmentId);
        return caseRepository.save(c);
    }

    @Override
    public PatientCase addFlightBooking(String caseId, String flightBookingId, BigDecimal amount) {
        PatientCase c = findByIdOrThrow(caseId);
        c.addFlightBooking(flightBookingId);
        c.addToTotal(amount);
        return caseRepository.save(c);
    }

    @Override
    public PatientCase addHotelBooking(String caseId, String hotelBookingId, BigDecimal amount) {
        PatientCase c = findByIdOrThrow(caseId);
        c.addHotelBooking(hotelBookingId);
        c.addToTotal(amount);
        return caseRepository.save(c);
    }

    @Override
    public PatientCase addPaymentRequest(String caseId, String paymentRequestId, BigDecimal totalImpact) {
        PatientCase c = findByIdOrThrow(caseId);
        c.addPaymentRequest(paymentRequestId);
        if (c.getStatus() == CaseStatus.OPEN || c.getStatus() == CaseStatus.IN_PROGRESS) {
            c.setStatus(CaseStatus.AWAITING_PAYMENT);
        }
        if (totalImpact != null) c.addToTotal(totalImpact);
        return caseRepository.save(c);
    }

    @Override
    public PatientCase registerPayment(String caseId, BigDecimal amount) {
        PatientCase c = findByIdOrThrow(caseId);
        c.registerPayment(amount);
        if (c.getPaidAmount() != null && c.getTotalAmount() != null
            && c.getPaidAmount().compareTo(c.getTotalAmount()) >= 0) {
            c.setStatus(CaseStatus.PAID);
        }
        return caseRepository.save(c);
    }

    @Override
    public PatientCase addConsent(String caseId, String consentId) {
        PatientCase c = findByIdOrThrow(caseId);
        c.addConsent(consentId);
        return caseRepository.save(c);
    }

    private String generateCaseNumber() {
        int year = LocalDate.now().getYear();
        long count = caseRepository.count() + 1;
        return String.format("HV-%d-%06d", year, count);
    }

    private CasePriority mapPriority(Lead.LeadPriority leadPriority) {
        if (leadPriority == null) return CasePriority.MEDIUM;
        try {
            return CasePriority.valueOf(leadPriority.name());
        } catch (IllegalArgumentException e) {
            return CasePriority.MEDIUM;
        }
    }
}
