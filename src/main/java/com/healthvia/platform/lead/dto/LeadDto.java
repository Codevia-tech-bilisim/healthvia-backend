// lead/dto/LeadDto.java
package com.healthvia.platform.lead.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDto {

    private String id;

    // İletişim
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String whatsappNumber;
    private String instagramHandle;
    private String contactDisplay;

    // Kaynak
    private LeadSource source;
    private String sourceDisplayName;
    private String sourceDetail;

    // Dil & konum
    private String language;
    private String country;
    private String city;

    // İlgi
    private String interestedTreatment;
    private String treatmentTypeId;
    private Set<String> interestedTreatments;
    private String budgetRange;
    private String preferredDates;
    private String initialMessage;
    private String notes;

    // Durum
    private LeadStatus status;
    private String statusDisplayName;
    private LeadStatus previousStatus;
    private LocalDateTime statusChangedAt;
    private String statusChangedBy;
    private LeadPriority priority;
    private String priorityDisplayName;

    // Atama
    private String assignedAgentId;
    private String assignedAgentName;
    private LocalDateTime assignedAt;
    private AssignmentMethod assignmentMethod;

    // Etiket
    private Set<String> tags;

    // Tekrar eden
    private Boolean isReturning;
    private String existingPatientId;

    // Dönüşüm
    private String conversationId;
    private String convertedPatientId;
    private LocalDateTime convertedAt;
    private BigDecimal conversionValue;
    private String lostReason;

    // Zamanlama
    private LocalDateTime firstResponseAt;
    private Long firstResponseTimeSeconds;
    private LocalDateTime lastContactAt;
    private LocalDateTime nextFollowUpAt;
    private Integer followUpCount;
    private Boolean needsFollowUp;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === FACTORY ===

    public static LeadDto fromEntity(Lead l) {
        if (l == null) return null;

        return LeadDto.builder()
                .id(l.getId())
                .firstName(l.getFirstName())
                .lastName(l.getLastName())
                .fullName(l.getFullName())
                .email(l.getEmail())
                .phone(l.getPhone())
                .whatsappNumber(l.getWhatsappNumber())
                .instagramHandle(l.getInstagramHandle())
                .contactDisplay(l.getContactDisplay())
                .source(l.getSource())
                .sourceDisplayName(l.getSource() != null ? l.getSource().getDisplayName() : null)
                .sourceDetail(l.getSourceDetail())
                .language(l.getLanguage())
                .country(l.getCountry())
                .city(l.getCity())
                .interestedTreatment(l.getInterestedTreatment())
                .treatmentTypeId(l.getTreatmentTypeId())
                .interestedTreatments(l.getInterestedTreatments())
                .budgetRange(l.getBudgetRange())
                .preferredDates(l.getPreferredDates())
                .initialMessage(l.getInitialMessage())
                .notes(l.getNotes())
                .status(l.getStatus())
                .statusDisplayName(l.getStatus() != null ? l.getStatus().getDisplayName() : null)
                .previousStatus(l.getPreviousStatus())
                .statusChangedAt(l.getStatusChangedAt())
                .statusChangedBy(l.getStatusChangedBy())
                .priority(l.getPriority())
                .priorityDisplayName(l.getPriority() != null ? l.getPriority().getDisplayName() : null)
                .assignedAgentId(l.getAssignedAgentId())
                .assignedAgentName(l.getAssignedAgentName())
                .assignedAt(l.getAssignedAt())
                .assignmentMethod(l.getAssignmentMethod())
                .tags(l.getTags())
                .isReturning(l.getIsReturning())
                .existingPatientId(l.getExistingPatientId())
                .conversationId(l.getConversationId())
                .convertedPatientId(l.getConvertedPatientId())
                .convertedAt(l.getConvertedAt())
                .conversionValue(l.getConversionValue())
                .lostReason(l.getLostReason())
                .firstResponseAt(l.getFirstResponseAt())
                .firstResponseTimeSeconds(l.getFirstResponseTimeSeconds())
                .lastContactAt(l.getLastContactAt())
                .nextFollowUpAt(l.getNextFollowUpAt())
                .followUpCount(l.getFollowUpCount())
                .needsFollowUp(l.needsFollowUp())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    /**
     * Inbox listesi için hafif versiyon
     */
    public static LeadDto fromEntityBasic(Lead l) {
        if (l == null) return null;

        return LeadDto.builder()
                .id(l.getId())
                .fullName(l.getFullName())
                .contactDisplay(l.getContactDisplay())
                .source(l.getSource())
                .sourceDisplayName(l.getSource() != null ? l.getSource().getDisplayName() : null)
                .language(l.getLanguage())
                .interestedTreatment(l.getInterestedTreatment())
                .status(l.getStatus())
                .statusDisplayName(l.getStatus() != null ? l.getStatus().getDisplayName() : null)
                .priority(l.getPriority())
                .priorityDisplayName(l.getPriority() != null ? l.getPriority().getDisplayName() : null)
                .assignedAgentId(l.getAssignedAgentId())
                .assignedAgentName(l.getAssignedAgentName())
                .tags(l.getTags())
                .isReturning(l.getIsReturning())
                .needsFollowUp(l.needsFollowUp())
                .lastContactAt(l.getLastContactAt())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
