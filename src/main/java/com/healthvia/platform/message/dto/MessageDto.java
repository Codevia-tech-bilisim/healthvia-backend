// message/dto/MessageDto.java
package com.healthvia.platform.message.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.entity.Message.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire shape for a chat message.
 *
 * Keeps backend-native fields (senderType, messageType, deliveryStatus,
 * readAt, isInternalNote) for internal callers while also exposing the
 * derived aliases the agent dashboard expects (direction, type, read,
 * authorId, authorName). The aliases are computed once in fromEntity so
 * the frontend never has to translate enum names or sender semantics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private String id;
    private String conversationId;

    // Gönderici (backend-native)
    private SenderType senderType;
    private String senderTypeDisplayName;
    private String senderId;
    private String senderName;

    // Gönderici (frontend alias)
    private String authorId;
    private String authorName;
    private String direction; // INBOUND | OUTBOUND

    // İçerik
    private MessageType messageType;
    private String messageTypeDisplayName;
    private String type; // frontend-friendly enum: TEXT | IMAGE | FILE | TEMPLATE | SYSTEM | NOTE
    private String content;
    private String contentHtml;
    private String preview;
    private List<AttachmentDto> attachments;

    // Şablon
    private String templateId;
    private String templateName;

    // Kanal
    private String channel;
    private String externalMessageId;

    // Durum
    private DeliveryStatus deliveryStatus;
    private String deliveryStatusDisplayName;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private Boolean read;
    private String failedReason;

    // Meta
    private Boolean isInternalNote;
    private Boolean isAutoReply;
    private String replyToMessageId;
    private Boolean isEdited;
    private LocalDateTime editedAt;

    // Audit
    private LocalDateTime createdAt;

    // === FACTORY ===

    public static MessageDto fromEntity(Message m) {
        if (m == null) return null;

        return MessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderType(m.getSenderType())
                .senderTypeDisplayName(m.getSenderType() != null ? m.getSenderType().getDisplayName() : null)
                .senderId(m.getSenderId())
                .senderName(m.getSenderName())
                .authorId(m.getSenderId())
                .authorName(m.getSenderName())
                .direction(resolveDirection(m))
                .messageType(m.getMessageType())
                .messageTypeDisplayName(m.getMessageType() != null ? m.getMessageType().getDisplayName() : null)
                .type(resolveType(m))
                .content(m.getContent())
                .contentHtml(m.getContentHtml())
                .preview(m.getPreview())
                .attachments(m.getAttachments() != null ?
                        m.getAttachments().stream().map(AttachmentDto::fromEntity).toList() : null)
                .templateId(m.getTemplateId())
                .templateName(m.getTemplateName())
                .channel(m.getChannel())
                .externalMessageId(m.getExternalMessageId())
                .deliveryStatus(m.getDeliveryStatus())
                .deliveryStatusDisplayName(m.getDeliveryStatus() != null ? m.getDeliveryStatus().getDisplayName() : null)
                .deliveredAt(m.getDeliveredAt())
                .readAt(m.getReadAt())
                .read(m.getReadAt() != null)
                .failedReason(m.getFailedReason())
                .isInternalNote(m.getIsInternalNote())
                .isAutoReply(m.getIsAutoReply())
                .replyToMessageId(m.getReplyToMessageId())
                .isEdited(m.getIsEdited())
                .editedAt(m.getEditedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private static String resolveDirection(Message m) {
        if (m.getSenderType() == null) return "INBOUND";
        return m.getSenderType() == SenderType.AGENT ? "OUTBOUND" : "INBOUND";
    }

    private static String resolveType(Message m) {
        if (Boolean.TRUE.equals(m.getIsInternalNote())) return "NOTE";
        MessageType t = m.getMessageType();
        if (t == null) return "TEXT";
        return switch (t) {
            case TEXT -> "TEXT";
            case IMAGE -> "IMAGE";
            case FILE, AUDIO, VIDEO -> "FILE";
            case TEMPLATE -> "TEMPLATE";
            case SYSTEM_EVENT -> "SYSTEM";
            case INTERNAL_NOTE -> "NOTE";
            case LOCATION -> "TEXT";
        };
    }

    // === NESTED ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private String fileName;
        private String fileUrl;
        private String mimeType;
        private Long fileSizeBytes;
        private String fileSizeDisplay;
        private String thumbnailUrl;

        public static AttachmentDto fromEntity(Message.Attachment a) {
            if (a == null) return null;
            return AttachmentDto.builder()
                    .fileName(a.getFileName())
                    .fileUrl(a.getFileUrl())
                    .mimeType(a.getMimeType())
                    .fileSizeBytes(a.getFileSizeBytes())
                    .fileSizeDisplay(a.getFileSizeDisplay())
                    .thumbnailUrl(a.getThumbnailUrl())
                    .build();
        }
    }
}
