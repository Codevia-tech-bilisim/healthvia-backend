package com.healthvia.platform.patientcase.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.repository.AppointmentRepository;
import com.healthvia.platform.booking.flight.entity.FlightBooking;
import com.healthvia.platform.booking.flight.repository.FlightBookingRepository;
import com.healthvia.platform.booking.hotel.entity.HotelBooking;
import com.healthvia.platform.booking.hotel.repository.HotelBookingRepository;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.service.LeadService;
import com.healthvia.platform.patientcase.dto.CaseFinancialSummaryDto;
import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.entity.PatientCase.CasePriority;
import com.healthvia.platform.patientcase.entity.PatientCase.CaseStatus;
import com.healthvia.platform.patientcase.repository.PatientCaseRepository;
import com.healthvia.platform.patientcase.service.PatientCaseService;
import com.healthvia.platform.payment.entity.PaymentRequest;
import com.healthvia.platform.payment.repository.PaymentRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PatientCaseServiceImpl implements PatientCaseService {

    private final PatientCaseRepository caseRepository;
    private final LeadService leadService;
    private final FlightBookingRepository flightBookingRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRequestRepository paymentRequestRepository;

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

    @Override
    public CaseFinancialSummaryDto getFinancialSummary(String caseId) {
        PatientCase c = findByIdOrThrow(caseId);

        var flightLines = flightBookingRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
            .map(this::flightToLine).toList();
        var hotelLines = hotelBookingRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
            .map(this::hotelToLine).toList();
        var apptLines = (c.getAppointmentIds() == null ? java.util.List.<String>of() : c.getAppointmentIds()).stream()
            .map(appointmentRepository::findByIdAndDeletedFalse)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .map(this::appointmentToLine).toList();
        var paymentLines = paymentRequestRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
            .map(this::paymentToLine).toList();

        java.math.BigDecimal pending = paymentRequestRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
            .filter(p -> p.getStatus() == PaymentRequest.PaymentStatus.LINK_SENT
                || p.getStatus() == PaymentRequest.PaymentStatus.PENDING)
            .map(p -> p.getAmount() == null ? java.math.BigDecimal.ZERO : p.getAmount())
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal total = c.getTotalAmount() == null ? java.math.BigDecimal.ZERO : c.getTotalAmount();
        java.math.BigDecimal paid = c.getPaidAmount() == null ? java.math.BigDecimal.ZERO : c.getPaidAmount();
        java.math.BigDecimal balance = total.subtract(paid);

        java.time.LocalDateTime started = c.getCreatedAt();
        java.time.LocalDateTime closed = c.getClosedAt();
        java.time.LocalDateTime end = closed != null ? closed : java.time.LocalDateTime.now();
        Long durationDays = started == null ? null : java.time.Duration.between(started, end).toDays();

        return CaseFinancialSummaryDto.builder()
            .caseId(c.getId())
            .caseNumber(c.getCaseNumber())
            .currency(c.getCurrency() == null ? "EUR" : c.getCurrency())
            .totalAmount(total)
            .paidAmount(paid)
            .pendingAmount(pending)
            .balance(balance)
            .journeyStartedAt(started)
            .journeyClosedAt(closed)
            .journeyDurationDays(durationDays)
            .flights(flightLines)
            .hotels(hotelLines)
            .appointments(apptLines)
            .payments(paymentLines)
            .build();
    }

    private CaseFinancialSummaryDto.LineItem flightToLine(FlightBooking f) {
        return CaseFinancialSummaryDto.LineItem.builder()
            .id(f.getId())
            .description(String.format("%s %s · %s → %s",
                f.getAirline(), f.getFlightNumber(),
                f.getDepartureAirportCode(), f.getArrivalAirportCode()))
            .amount(f.getPrice())
            .currency(f.getCurrency())
            .status(f.getStatus() == null ? null : f.getStatus().name())
            .occurredAt(f.getDepartureTime())
            .build();
    }

    private CaseFinancialSummaryDto.LineItem hotelToLine(HotelBooking h) {
        return CaseFinancialSummaryDto.LineItem.builder()
            .id(h.getId())
            .description(String.format("%s · %s · %d gece",
                h.getHotelName(), h.getRoomType(), h.getNights() == null ? 0 : h.getNights()))
            .amount(h.getTotalPrice())
            .currency(h.getCurrency())
            .status(h.getStatus() == null ? null : h.getStatus().name())
            .occurredAt(h.getCheckIn() == null ? null : h.getCheckIn().atStartOfDay())
            .build();
    }

    private CaseFinancialSummaryDto.LineItem appointmentToLine(Appointment a) {
        java.time.LocalDateTime when = (a.getAppointmentDate() == null) ? null
            : a.getAppointmentDate().atTime(a.getStartTime() == null ? java.time.LocalTime.NOON : a.getStartTime());
        return CaseFinancialSummaryDto.LineItem.builder()
            .id(a.getId())
            .description(String.format("Randevu — %s", a.getDoctorName() == null ? "—" : a.getDoctorName()))
            .amount(a.getTotalPrice() == null ? a.getConsultationFee() : a.getTotalPrice())
            .currency("TRY")
            .status(a.getStatus() == null ? null : a.getStatus().name())
            .occurredAt(when)
            .build();
    }

    private CaseFinancialSummaryDto.PaymentLine paymentToLine(PaymentRequest p) {
        return CaseFinancialSummaryDto.PaymentLine.builder()
            .id(p.getId())
            .method(p.getMethod() == null ? null : p.getMethod().name())
            .status(p.getStatus() == null ? null : p.getStatus().name())
            .amount(p.getAmount())
            .currency(p.getCurrency())
            .description(p.getDescription())
            .linkUrl(p.getLinkUrl())
            .sentAt(p.getLinkSentAt())
            .paidAt(p.getPaidAt())
            .expiresAt(p.getLinkExpiresAt())
            .build();
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
