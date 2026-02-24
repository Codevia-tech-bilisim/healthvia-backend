package com.healthvia.platform.reminder.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.healthvia.platform.reminder.entity.Reminder;
import com.healthvia.platform.reminder.entity.Reminder.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderDto {

    private String id;

    // Referanslar
    private String conversationId;
    private String leadId;
    private String patientId;
    private String ticketId;
    private String appointmentId;

    // Temel
    private String title;
    private String note;
    private ReminderType type;
    private String typeDisplayName;
    private LocalDateTime remindAt;

    // Durum
    private ReminderStatus status;
    private String statusDisplayName;
    private LocalDateTime completedAt;
    private String completedBy;
    private LocalDateTime snoozedUntil;
    private Integer snoozeCount;

    // Atama
    private String assignedAgentId;
    private String assignedAgentName;
    private String createdByAgentId;
    private String createdByAgentName;

    // Öncelik
    private ReminderPriority priority;
    private String priorityDisplayName;

    // Tekrarlama
    private Boolean isRecurring;
    private RecurrenceRule recurrenceRule;
    private String recurrenceDisplayName;
    private LocalDateTime nextOccurrence;
    private Integer occurrenceCount;
    private Integer maxOccurrences;

    // Bildirim
    private Set<String> notificationChannels;
    private Boolean notificationSent;

    // Computed
    private Boolean isDue;
    private Boolean isMissed;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === FACTORY ===

    public static ReminderDto fromEntity(Reminder r) {
        if (r == null) return null;

        return ReminderDto.builder()
                .id(r.getId())
                .conversationId(r.getConversationId())
                .leadId(r.getLeadId())
                .patientId(r.getPatientId())
                .ticketId(r.getTicketId())
                .appointmentId(r.getAppointmentId())
                .title(r.getTitle())
                .note(r.getNote())
                .type(r.getType())
                .typeDisplayName(r.getType() != null ? r.getType().getDisplayName() : null)
                .remindAt(r.getRemindAt())
                .status(r.getStatus())
                .statusDisplayName(r.getStatus() != null ? r.getStatus().getDisplayName() : null)
                .completedAt(r.getCompletedAt())
                .completedBy(r.getCompletedBy())
                .snoozedUntil(r.getSnoozedUntil())
                .snoozeCount(r.getSnoozeCount())
                .assignedAgentId(r.getAssignedAgentId())
                .assignedAgentName(r.getAssignedAgentName())
                .createdByAgentId(r.getCreatedByAgentId())
                .createdByAgentName(r.getCreatedByAgentName())
                .priority(r.getPriority())
                .priorityDisplayName(r.getPriority() != null ? r.getPriority().getDisplayName() : null)
                .isRecurring(r.getIsRecurring())
                .recurrenceRule(r.getRecurrenceRule())
                .recurrenceDisplayName(r.getRecurrenceRule() != null ? r.getRecurrenceRule().getDisplayName() : null)
                .nextOccurrence(r.getNextOccurrence())
                .occurrenceCount(r.getOccurrenceCount())
                .maxOccurrences(r.getMaxOccurrences())
                .notificationChannels(r.getNotificationChannels())
                .notificationSent(r.getNotificationSent())
                .isDue(r.isDue())
                .isMissed(r.isMissed())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    /**
     * Liste görünümü — hafif versiyon
     */
    public static ReminderDto fromEntityBasic(Reminder r) {
        if (r == null) return null;

        return ReminderDto.builder()
                .id(r.getId())
                .conversationId(r.getConversationId())
                .leadId(r.getLeadId())
                .title(r.getTitle())
                .type(r.getType())
                .typeDisplayName(r.getType() != null ? r.getType().getDisplayName() : null)
                .remindAt(r.getRemindAt())
                .status(r.getStatus())
                .statusDisplayName(r.getStatus() != null ? r.getStatus().getDisplayName() : null)
                .priority(r.getPriority())
                .assignedAgentId(r.getAssignedAgentId())
                .assignedAgentName(r.getAssignedAgentName())
                .isRecurring(r.getIsRecurring())
                .isDue(r.isDue())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
