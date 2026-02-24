package com.healthvia.platform.reminder.entity;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.model.BaseEntity;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "reminders")
@CompoundIndex(def = "{'assignedAgentId': 1, 'status': 1, 'remindAt': 1}")
@CompoundIndex(def = "{'status': 1, 'remindAt': 1}")
public class Reminder extends BaseEntity {

    // === REFERANSLAR ===

    @Field("conversation_id")
    @Indexed
    private String conversationId;

    @Field("lead_id")
    @Indexed
    private String leadId;

    @Field("patient_id")
    private String patientId;

    @Field("ticket_id")
    private String ticketId;

    @Field("appointment_id")
    private String appointmentId;

    // === TEMEL BİLGİLER ===

    @NotBlank(message = "Hatırlatıcı başlığı boş olamaz")
    @Size(max = 300)
    private String title;

    @Size(max = 2000)
    private String note;

    @NotNull(message = "Hatırlatıcı tipi belirtilmelidir")
    @Indexed
    private ReminderType type;

    @NotNull(message = "Hatırlatma zamanı belirtilmelidir")
    @Field("remind_at")
    @Indexed
    private LocalDateTime remindAt;

    // === DURUM ===

    @NotNull
    @Indexed
    private ReminderStatus status;

    @Field("completed_at")
    private LocalDateTime completedAt;

    @Field("completed_by")
    private String completedBy;

    @Field("snoozed_until")
    private LocalDateTime snoozedUntil;

    @Field("snooze_count")
    private Integer snoozeCount;

    // === ATAMA ===

    @NotBlank(message = "Hatırlatıcı sahibi belirtilmelidir")
    @Field("assigned_agent_id")
    @Indexed
    private String assignedAgentId;

    @Field("assigned_agent_name")
    private String assignedAgentName;

    @Field("created_by_agent_id")
    private String createdByAgentId;

    @Field("created_by_agent_name")
    private String createdByAgentName;

    // === ÖNCELİK ===

    private ReminderPriority priority;

    // === TEKRARLama ===

    @Field("is_recurring")
    private Boolean isRecurring;

    @Field("recurrence_rule")
    private RecurrenceRule recurrenceRule;

    @Field("next_occurrence")
    private LocalDateTime nextOccurrence;

    @Field("occurrence_count")
    private Integer occurrenceCount;

    @Field("max_occurrences")
    private Integer maxOccurrences;

    // === BİLDİRİM ===

    @Field("notification_channels")
    private Set<String> notificationChannels; // "IN_APP", "EMAIL", "SMS"

    @Field("notification_sent")
    private Boolean notificationSent;

    @Field("notification_sent_at")
    private LocalDateTime notificationSentAt;

    // === ENUMS ===

    public enum ReminderType {
        FOLLOW_UP("Takip Araması"),
        CALLBACK("Geri Arama"),
        APPOINTMENT_PREP("Randevu Hazırlığı"),
        DOCUMENT_REQUEST("Doküman Talebi"),
        PAYMENT_FOLLOW_UP("Ödeme Takibi"),
        HOTEL_CHECK("Otel Onayı"),
        FLIGHT_CHECK("Uçuş Onayı"),
        TRANSFER_ARRANGE("Transfer Düzenleme"),
        POST_TREATMENT("Tedavi Sonrası Kontrol"),
        VISA_FOLLOW_UP("Vize Takibi"),
        FEEDBACK_REQUEST("Geri Bildirim Talebi"),
        CUSTOM("Özel");

        private final String displayName;
        ReminderType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ReminderStatus {
        PENDING("Bekliyor"),
        TRIGGERED("Tetiklendi"),
        SNOOZED("Ertelendi"),
        COMPLETED("Tamamlandı"),
        CANCELLED("İptal"),
        MISSED("Kaçırıldı");

        private final String displayName;
        ReminderStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ReminderPriority {
        LOW("Düşük"),
        NORMAL("Normal"),
        HIGH("Yüksek"),
        URGENT("Acil");

        private final String displayName;
        ReminderPriority(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum RecurrenceRule {
        DAILY("Günlük"),
        EVERY_OTHER_DAY("İki Günde Bir"),
        WEEKLY("Haftalık"),
        BIWEEKLY("İki Haftada Bir"),
        MONTHLY("Aylık");

        private final String displayName;
        RecurrenceRule(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === HELPER METHODS ===

    public boolean isDue() {
        if (status != ReminderStatus.PENDING) return false;
        if (snoozedUntil != null && LocalDateTime.now().isBefore(snoozedUntil)) return false;
        return LocalDateTime.now().isAfter(remindAt) || LocalDateTime.now().isEqual(remindAt);
    }

    public boolean isMissed() {
        return status == ReminderStatus.TRIGGERED &&
               remindAt.plusHours(24).isBefore(LocalDateTime.now());
    }

    public Boolean getIsRecurring() {
        return isRecurring != null ? isRecurring : false;
    }

    public Integer getSnoozeCount() {
        return snoozeCount != null ? snoozeCount : 0;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount != null ? occurrenceCount : 0;
    }

    public Boolean getNotificationSent() {
        return notificationSent != null ? notificationSent : false;
    }

    /**
     * Recurring reminder için sonraki zamanı hesapla
     */
    public LocalDateTime calculateNextOccurrence() {
        if (!getIsRecurring() || recurrenceRule == null) return null;
        LocalDateTime base = remindAt;
        return switch (recurrenceRule) {
            case DAILY -> base.plusDays(1);
            case EVERY_OTHER_DAY -> base.plusDays(2);
            case WEEKLY -> base.plusWeeks(1);
            case BIWEEKLY -> base.plusWeeks(2);
            case MONTHLY -> base.plusMonths(1);
        };
    }
}
