// conversation/entity/Conversation.java
package com.healthvia.platform.conversation.entity;

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
@Document(collection = "conversations")
@CompoundIndex(def = "{'leadId': 1, 'status': 1}")
@CompoundIndex(def = "{'assignedAgentId': 1, 'status': 1}")
@CompoundIndex(def = "{'channel': 1, 'status': 1}")
public class Conversation extends BaseEntity {

    // === İLİŞKİLER ===

    @NotBlank(message = "Lead ID boş olamaz")
    @Field("lead_id")
    @Indexed
    private String leadId;

    @Field("patient_id")
    private String patientId; // Lead dönüştürüldüyse

    @Field("assigned_agent_id")
    @Indexed
    private String assignedAgentId;

    @Field("assigned_agent_name")
    private String assignedAgentName;

    // === KANAL ===

    @NotNull(message = "Kanal belirtilmelidir")
    @Indexed
    private Channel channel;

    @Field("channel_conversation_id")
    private String channelConversationId; // WhatsApp thread ID, Instagram conversation ID vb.

    // === DURUM ===

    @NotNull(message = "Konuşma durumu belirtilmelidir")
    @Indexed
    private ConversationStatus status;

    @Field("previous_status")
    private ConversationStatus previousStatus;

    @Field("status_changed_at")
    private LocalDateTime statusChangedAt;

    // === ÖZET BİLGİLER ===

    @Size(max = 200)
    private String subject; // Konuşma konusu

    @Field("last_message_preview")
    @Size(max = 300)
    private String lastMessagePreview;

    @Field("last_message_at")
    @Indexed
    private LocalDateTime lastMessageAt;

    @Field("last_message_sender")
    private String lastMessageSender; // "AGENT" veya "LEAD"

    @Field("total_messages")
    private Integer totalMessages;

    @Field("unread_count")
    private Integer unreadCount; // Agent tarafından okunmamış mesajlar

    // === ETİKETLER & KATEGORİ ===

    private Set<String> tags;

    @Field("treatment_interest")
    private String treatmentInterest; // Konuşmadaki tedavi ilgisi

    private String language;

    // === ÖNCELİK ===

    private ConversationPriority priority;

    // === İLİŞKİLİ KAYITLAR ===

    @Field("ticket_ids")
    private Set<String> ticketIds;

    @Field("reminder_ids")
    private Set<String> reminderIds;

    @Field("appointment_id")
    private String appointmentId;

    // === ZAMANLAMA ===

    @Field("first_response_at")
    private LocalDateTime firstResponseAt;

    @Field("resolved_at")
    private LocalDateTime resolvedAt;

    @Field("archived_at")
    private LocalDateTime archivedAt;

    @Field("is_pinned")
    private Boolean isPinned;

    // === ENUMS ===

    public enum Channel {
        WHATSAPP("WhatsApp"),
        INSTAGRAM("Instagram"),
        EMAIL("Email"),
        TELEGRAM("Telegram"),
        LIVE_CHAT("Canlı Chat"),
        PHONE("Telefon"),
        SMS("SMS"),
        WEB_FORM("Web Form"),
        INTERNAL("Dahili Not");

        private final String displayName;
        Channel(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ConversationStatus {
        OPEN("Açık"),
        WAITING_REPLY("Yanıt Bekleniyor"),    // Hasta yanıtı bekleniyor
        AGENT_REPLY("Agent Yanıtı Gerekli"),  // Agent yanıt vermeli
        ON_HOLD("Beklemede"),
        RESOLVED("Çözüldü"),
        ARCHIVED("Arşiv");

        private final String displayName;
        ConversationStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ConversationPriority {
        LOW("Düşük"),
        NORMAL("Normal"),
        HIGH("Yüksek"),
        URGENT("Acil");

        private final String displayName;
        ConversationPriority(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === HELPER METHODS ===

    public boolean isOpen() {
        return status != null && !Set.of(
                ConversationStatus.RESOLVED, ConversationStatus.ARCHIVED
        ).contains(status);
    }

    public void addMessage(String preview, String sender) {
        this.lastMessagePreview = preview;
        this.lastMessageAt = LocalDateTime.now();
        this.lastMessageSender = sender;
        this.totalMessages = getTotalMessages() + 1;
        if ("LEAD".equals(sender)) {
            this.unreadCount = getUnreadCount() + 1;
        }
    }

    public void markAsRead() {
        this.unreadCount = 0;
    }

    public Integer getTotalMessages() {
        return totalMessages != null ? totalMessages : 0;
    }

    public Integer getUnreadCount() {
        return unreadCount != null ? unreadCount : 0;
    }

    public Boolean getIsPinned() {
        return isPinned != null ? isPinned : false;
    }
}
