// message/entity/Message.java
package com.healthvia.platform.message.entity;

import java.time.LocalDateTime;
import java.util.List;

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
@Document(collection = "messages")
@CompoundIndex(def = "{'conversationId': 1, 'createdAt': 1}")
@CompoundIndex(def = "{'senderId': 1, 'createdAt': -1}")
public class Message extends BaseEntity {

    // === İLİŞKİLER ===

    @NotBlank(message = "Konuşma ID boş olamaz")
    @Field("conversation_id")
    @Indexed
    private String conversationId;

    @Field("lead_id")
    @Indexed
    private String leadId;

    // === GÖNDERİCİ ===

    @NotNull(message = "Gönderici tipi belirtilmelidir")
    @Field("sender_type")
    private SenderType senderType;

    @Field("sender_id")
    @Indexed
    private String senderId; // Agent ID veya Lead ID

    @Field("sender_name")
    private String senderName;

    // === İÇERİK ===

    @NotNull(message = "Mesaj tipi belirtilmelidir")
    @Field("message_type")
    private MessageType messageType;

    @Size(max = 10000, message = "Mesaj en fazla 10000 karakter olabilir")
    private String content; // Metin içerik

    @Field("content_html")
    private String contentHtml; // Zengin içerik (email vb.)

    // === EK DOSYALAR ===

    private List<Attachment> attachments;

    // === ŞABLON ===

    @Field("template_id")
    private String templateId; // Kullanılan şablon

    @Field("template_name")
    private String templateName;

    // === KANAL BİLGİSİ ===

    @Field("channel")
    private String channel; // "WHATSAPP", "EMAIL" vb.

    @Field("external_message_id")
    private String externalMessageId; // WhatsApp message ID, Email message-id vb.

    // === DURUM ===

    @Field("delivery_status")
    private DeliveryStatus deliveryStatus;

    @Field("delivered_at")
    private LocalDateTime deliveredAt;

    @Field("read_at")
    private LocalDateTime readAt;

    @Field("failed_reason")
    private String failedReason;

    // === METADATA ===

    @Field("is_internal_note")
    private Boolean isInternalNote; // Sadece agentlerin görebildiği not

    @Field("is_auto_reply")
    private Boolean isAutoReply; // Otomatik yanıt mı

    @Field("reply_to_message_id")
    private String replyToMessageId; // Yanıt verilen mesaj

    @Field("is_edited")
    private Boolean isEdited;

    @Field("edited_at")
    private LocalDateTime editedAt;

    @Field("original_content")
    private String originalContent; // Düzenlenmeden önceki içerik

    // === ENUMS ===

    public enum SenderType {
        AGENT("Agent"),
        LEAD("Lead / Hasta"),
        SYSTEM("Sistem"),
        BOT("Bot");

        private final String displayName;
        SenderType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum MessageType {
        TEXT("Metin"),
        IMAGE("Görsel"),
        FILE("Dosya"),
        AUDIO("Ses"),
        VIDEO("Video"),
        LOCATION("Konum"),
        TEMPLATE("Şablon"),
        SYSTEM_EVENT("Sistem Olayı"), // "Agent değişti", "Konuşma açıldı" vb.
        INTERNAL_NOTE("Dahili Not");

        private final String displayName;
        MessageType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum DeliveryStatus {
        PENDING("Gönderiliyor"),
        SENT("Gönderildi"),
        DELIVERED("İletildi"),
        READ("Okundu"),
        FAILED("Başarısız");

        private final String displayName;
        DeliveryStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === NESTED ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String fileName;
        private String fileUrl;
        private String mimeType;
        private Long fileSizeBytes;
        private String thumbnailUrl;

        public String getFileSizeDisplay() {
            if (fileSizeBytes == null) return "Bilinmiyor";
            if (fileSizeBytes < 1024) return fileSizeBytes + " B";
            if (fileSizeBytes < 1024 * 1024) return (fileSizeBytes / 1024) + " KB";
            return (fileSizeBytes / (1024 * 1024)) + " MB";
        }
    }

    // === HELPER METHODS ===

    public boolean isFromAgent() {
        return SenderType.AGENT.equals(senderType);
    }

    public boolean isFromLead() {
        return SenderType.LEAD.equals(senderType);
    }

    public boolean isSystemMessage() {
        return SenderType.SYSTEM.equals(senderType) || MessageType.SYSTEM_EVENT.equals(messageType);
    }

    public Boolean getIsInternalNote() {
        return isInternalNote != null ? isInternalNote : false;
    }

    public Boolean getIsAutoReply() {
        return isAutoReply != null ? isAutoReply : false;
    }

    public Boolean getIsEdited() {
        return isEdited != null ? isEdited : false;
    }

    public String getPreview() {
        if (content == null) return messageType != null ? messageType.getDisplayName() : "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}
