package com.healthvia.platform.patientcase.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.healthvia.platform.patientcase.entity.PatientCase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCaseDto {
    private String id;
    private String caseNumber;
    private String patientId;
    private String leadId;
    private String assignedAgentId;
    private List<String> previousAgentIds;
    private PatientCase.CaseStatus status;
    private PatientCase.CasePriority priority;
    private String treatmentTypeId;
    private List<String> appointmentIds;
    private List<String> flightBookingIds;
    private List<String> hotelBookingIds;
    private List<String> paymentRequestIds;
    private List<String> ticketIds;
    private String conversationId;
    private Boolean createdOnBehalf;
    private List<String> consentRecordIds;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String currency;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    public static PatientCaseDto fromEntity(PatientCase c) {
        if (c == null) return null;
        return PatientCaseDto.builder()
            .id(c.getId())
            .caseNumber(c.getCaseNumber())
            .patientId(c.getPatientId())
            .leadId(c.getLeadId())
            .assignedAgentId(c.getAssignedAgentId())
            .previousAgentIds(c.getPreviousAgentIds())
            .status(c.getStatus())
            .priority(c.getPriority())
            .treatmentTypeId(c.getTreatmentTypeId())
            .appointmentIds(c.getAppointmentIds())
            .flightBookingIds(c.getFlightBookingIds())
            .hotelBookingIds(c.getHotelBookingIds())
            .paymentRequestIds(c.getPaymentRequestIds())
            .ticketIds(c.getTicketIds())
            .conversationId(c.getConversationId())
            .createdOnBehalf(c.getCreatedOnBehalf())
            .consentRecordIds(c.getConsentRecordIds())
            .totalAmount(c.getTotalAmount())
            .paidAmount(c.getPaidAmount())
            .currency(c.getCurrency())
            .notes(c.getNotes())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .closedAt(c.getClosedAt())
            .build();
    }
}
