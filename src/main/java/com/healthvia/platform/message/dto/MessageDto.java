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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private String id;
    private String conversationId;

    // Gönderici
    private SenderType senderType;
    private String senderTypeDisplayName;
    private String senderId;
    private String senderName;

    // İçerik
    private MessageType messageType;
    private String messageTypeDisplayName;
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
                .messageType(m.getMessageType())
                .messageTypeDisplayName(m.getMessageType() != null ? m.getMessageType().getDisplayName() : null)
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
                .failedReason(m.getFailedReason())
                .isInternalNote(m.getIsInternalNote())
                .isAutoReply(m.getIsAutoReply())
                .replyToMessageId(m.getReplyToMessageId())
                .isEdited(m.getIsEdited())
                .editedAt(m.getEditedAt())
                .createdAt(m.getCreatedAt())
                .build();
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
