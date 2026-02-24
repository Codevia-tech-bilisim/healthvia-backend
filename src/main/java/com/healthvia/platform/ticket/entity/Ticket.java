package com.healthvia.platform.ticket.entity;

import java.time.LocalDateTime;
import java.util.List;
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
@Document(collection = "tickets")
@CompoundIndex(def = "{'conversationId': 1, 'status': 1}")
@CompoundIndex(def = "{'assignedAgentId': 1, 'status': 1}")
@CompoundIndex(def = "{'category': 1, 'status': 1, 'priority': 1}")
public class Ticket extends BaseEntity {

    // === REFERANSLAR ===

    @Field("conversation_id")
    @Indexed
    private String conversationId;

    @Field("lead_id")
    @Indexed
    private String leadId;

    @Field("patient_id")
    private String patientId;

    // === TEMEL BİLGİLER ===

    @NotBlank(message = "Ticket başlığı boş olamaz")
    @Size(max = 300)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Kategori belirtilmelidir")
    @Indexed
    private TicketCategory category;

    @NotNull(message = "Öncelik belirtilmelidir")
    private TicketPriority priority;

    @NotNull(message = "Durum belirtilmelidir")
    @Indexed
    private TicketStatus status;

    @Field("previous_status")
    private TicketStatus previousStatus;

    @Field("status_changed_at")
    private LocalDateTime statusChangedAt;

    // === ATAMA ===

    @Field("assigned_agent_id")
    @Indexed
    private String assignedAgentId;

    @Field("assigned_agent_name")
    private String assignedAgentName;

    @Field("assigned_at")
    private LocalDateTime assignedAt;

    @Field("reported_by_agent_id")
    private String reportedByAgentId;

    @Field("reported_by_agent_name")
    private String reportedByAgentName;

    // === İLİŞKİLİ KAYITLAR ===

    @Field("appointment_id")
    private String appointmentId;

    @Field("hotel_booking_id")
    private String hotelBookingId;

    @Field("flight_booking_id")
    private String flightBookingId;

    @Field("treatment_type_id")
    private String treatmentTypeId;

    // === DETAYLAR ===

    @Field("due_date")
    @Indexed
    private LocalDateTime dueDate;

    private Set<String> tags;

    @Field("checklist")
    private List<ChecklistItem> checklist;

    @Field("activity_log")
    private List<ActivityEntry> activityLog;

    @Field("attachment_urls")
    private List<String> attachmentUrls;

    // === ÇÖZÜM ===

    @Field("resolution_note")
    @Size(max = 2000)
    private String resolutionNote;

    @Field("resolved_at")
    private LocalDateTime resolvedAt;

    @Field("resolved_by_agent_id")
    private String resolvedByAgentId;

    @Field("closed_at")
    private LocalDateTime closedAt;

    // === SLA ===

    @Field("sla_deadline")
    private LocalDateTime slaDeadline;

    @Field("sla_breached")
    private Boolean slaBreached;

    // === ENUMS ===

    public enum TicketCategory {
        HOTEL_BOOKING("Otel Rezervasyonu"),
        FLIGHT_BOOKING("Uçuş Rezervasyonu"),
        TRANSFER("Transfer / Ulaşım"),
        APPOINTMENT("Randevu İşlemi"),
        DOCUMENT("Doküman / Vize"),
        PAYMENT("Ödeme"),
        TRANSLATION("Tercümanlık"),
        MEDICAL_RECORD("Tıbbi Kayıt"),
        INSURANCE("Sigorta"),
        COMPLAINT("Şikayet"),
        FEEDBACK("Geri Bildirim"),
        GENERAL("Genel"),
        OTHER("Diğer");

        private final String displayName;
        TicketCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum TicketPriority {
        LOW("Düşük", 72),
        NORMAL("Normal", 48),
        HIGH("Yüksek", 24),
        URGENT("Acil", 4);

        private final String displayName;
        private final int slaHours; // Varsayılan SLA süresi (saat)
        TicketPriority(String displayName, int slaHours) {
            this.displayName = displayName;
            this.slaHours = slaHours;
        }
        public String getDisplayName() { return displayName; }
        public int getSlaHours() { return slaHours; }
    }

    public enum TicketStatus {
        OPEN("Açık"),
        IN_PROGRESS("İşlemde"),
        WAITING_CUSTOMER("Müşteri Yanıtı Bekleniyor"),
        WAITING_EXTERNAL("Dış Kurum Yanıtı Bekleniyor"),
        ON_HOLD("Beklemede"),
        RESOLVED("Çözüldü"),
        CLOSED("Kapatıldı"),
        CANCELLED("İptal");

        private final String displayName;
        TicketStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === NESTED CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItem {
        private String text;
        private Boolean completed;
        private LocalDateTime completedAt;
        private String completedBy;

        public Boolean getCompleted() {
            return completed != null ? completed : false;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityEntry {
        private LocalDateTime timestamp;
        private String agentId;
        private String agentName;
        private String action; // "STATUS_CHANGED", "ASSIGNED", "COMMENT", "CHECKLIST_UPDATED"
        private String detail; // "Durum: OPEN → IN_PROGRESS"

        public static ActivityEntry create(String agentId, String agentName, String action, String detail) {
            ActivityEntry entry = new ActivityEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setAgentId(agentId);
            entry.setAgentName(agentName);
            entry.setAction(action);
            entry.setDetail(detail);
            return entry;
        }
    }

    // === HELPER METHODS ===

    public boolean isOpen() {
        return status != null && !Set.of(
                TicketStatus.RESOLVED, TicketStatus.CLOSED, TicketStatus.CANCELLED
        ).contains(status);
    }

    public boolean isOverdue() {
        if (dueDate == null) return false;
        return isOpen() && LocalDateTime.now().isAfter(dueDate);
    }

    public boolean isSlaBreached() {
        if (slaBreached != null && slaBreached) return true;
        if (slaDeadline == null) return false;
        return isOpen() && LocalDateTime.now().isAfter(slaDeadline);
    }

    public int getChecklistProgress() {
        if (checklist == null || checklist.isEmpty()) return 0;
        long completed = checklist.stream().filter(c -> c.getCompleted()).count();
        return (int) ((completed * 100) / checklist.size());
    }

    public void addActivity(String agentId, String agentName, String action, String detail) {
        if (activityLog == null) activityLog = new java.util.ArrayList<>();
        activityLog.add(ActivityEntry.create(agentId, agentName, action, detail));
    }

    public Boolean getSlaBreached() {
        return slaBreached != null ? slaBreached : false;
    }
}
