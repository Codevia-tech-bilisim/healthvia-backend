package com.healthvia.platform.ticket.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.ticket.entity.Ticket;
import com.healthvia.platform.ticket.entity.Ticket.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDto {

    private String id;

    // Referanslar
    private String conversationId;
    private String leadId;
    private String patientId;

    // Temel
    private String title;
    private String description;
    private TicketCategory category;
    private String categoryDisplayName;
    private TicketPriority priority;
    private String priorityDisplayName;
    private TicketStatus status;
    private String statusDisplayName;

    // Atama
    private String assignedAgentId;
    private String assignedAgentName;
    private LocalDateTime assignedAt;
    private String reportedByAgentId;
    private String reportedByAgentName;

    // İlişkili kayıtlar
    private String appointmentId;
    private String hotelBookingId;
    private String flightBookingId;
    private String treatmentTypeId;

    // Detaylar
    private LocalDateTime dueDate;
    private Set<String> tags;
    private List<ChecklistItemDto> checklist;
    private int checklistProgress;
    private List<String> attachmentUrls;

    // Çözüm
    private String resolutionNote;
    private LocalDateTime resolvedAt;

    // SLA
    private LocalDateTime slaDeadline;
    private Boolean slaBreached;
    private Boolean overdue;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Activity log (sadece detay görünümde)
    private List<ActivityEntryDto> activityLog;

    // === FACTORY ===

    public static TicketDto fromEntity(Ticket t) {
        if (t == null) return null;

        return TicketDto.builder()
                .id(t.getId())
                .conversationId(t.getConversationId())
                .leadId(t.getLeadId())
                .patientId(t.getPatientId())
                .title(t.getTitle())
                .description(t.getDescription())
                .category(t.getCategory())
                .categoryDisplayName(t.getCategory() != null ? t.getCategory().getDisplayName() : null)
                .priority(t.getPriority())
                .priorityDisplayName(t.getPriority() != null ? t.getPriority().getDisplayName() : null)
                .status(t.getStatus())
                .statusDisplayName(t.getStatus() != null ? t.getStatus().getDisplayName() : null)
                .assignedAgentId(t.getAssignedAgentId())
                .assignedAgentName(t.getAssignedAgentName())
                .assignedAt(t.getAssignedAt())
                .reportedByAgentId(t.getReportedByAgentId())
                .reportedByAgentName(t.getReportedByAgentName())
                .appointmentId(t.getAppointmentId())
                .hotelBookingId(t.getHotelBookingId())
                .flightBookingId(t.getFlightBookingId())
                .treatmentTypeId(t.getTreatmentTypeId())
                .dueDate(t.getDueDate())
                .tags(t.getTags())
                .checklist(t.getChecklist() != null ?
                        t.getChecklist().stream().map(ChecklistItemDto::fromEntity).toList() : null)
                .checklistProgress(t.getChecklistProgress())
                .attachmentUrls(t.getAttachmentUrls())
                .resolutionNote(t.getResolutionNote())
                .resolvedAt(t.getResolvedAt())
                .slaDeadline(t.getSlaDeadline())
                .slaBreached(t.isSlaBreached())
                .overdue(t.isOverdue())
                .activityLog(t.getActivityLog() != null ?
                        t.getActivityLog().stream().map(ActivityEntryDto::fromEntity).toList() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    /**
     * Liste görünümü — activity log hariç, hafif
     */
    public static TicketDto fromEntityBasic(Ticket t) {
        if (t == null) return null;

        return TicketDto.builder()
                .id(t.getId())
                .conversationId(t.getConversationId())
                .leadId(t.getLeadId())
                .title(t.getTitle())
                .category(t.getCategory())
                .categoryDisplayName(t.getCategory() != null ? t.getCategory().getDisplayName() : null)
                .priority(t.getPriority())
                .priorityDisplayName(t.getPriority() != null ? t.getPriority().getDisplayName() : null)
                .status(t.getStatus())
                .statusDisplayName(t.getStatus() != null ? t.getStatus().getDisplayName() : null)
                .assignedAgentId(t.getAssignedAgentId())
                .assignedAgentName(t.getAssignedAgentName())
                .dueDate(t.getDueDate())
                .checklistProgress(t.getChecklistProgress())
                .slaBreached(t.isSlaBreached())
                .overdue(t.isOverdue())
                .tags(t.getTags())
                .createdAt(t.getCreatedAt())
                .build();
    }

    // === NESTED DTOs ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItemDto {
        private String text;
        private Boolean completed;
        private LocalDateTime completedAt;
        private String completedBy;

        public static ChecklistItemDto fromEntity(ChecklistItem item) {
            if (item == null) return null;
            return ChecklistItemDto.builder()
                    .text(item.getText())
                    .completed(item.getCompleted())
                    .completedAt(item.getCompletedAt())
                    .completedBy(item.getCompletedBy())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityEntryDto {
        private LocalDateTime timestamp;
        private String agentId;
        private String agentName;
        private String action;
        private String detail;

        public static ActivityEntryDto fromEntity(ActivityEntry entry) {
            if (entry == null) return null;
            return ActivityEntryDto.builder()
                    .timestamp(entry.getTimestamp())
                    .agentId(entry.getAgentId())
                    .agentName(entry.getAgentName())
                    .action(entry.getAction())
                    .detail(entry.getDetail())
                    .build();
        }
    }
}
