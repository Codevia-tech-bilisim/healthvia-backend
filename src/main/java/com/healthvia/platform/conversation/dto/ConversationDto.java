// conversation/dto/ConversationDto.java
package com.healthvia.platform.conversation.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private String id;

    // İlişkiler
    private String leadId;
    private String patientId;
    private String assignedAgentId;
    private String assignedAgentName;

    // Kanal
    private Channel channel;
    private String channelDisplayName;
    private String channelConversationId;

    // Durum
    private ConversationStatus status;
    private String statusDisplayName;
    private ConversationPriority priority;
    private String priorityDisplayName;

    // Özet
    private String subject;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private String lastMessageSender;
    private Integer totalMessages;
    private Integer unreadCount;

    // Etiket & kategori
    private Set<String> tags;
    private String treatmentInterest;
    private String language;

    // İlişkili kayıtlar
    private Set<String> ticketIds;
    private Set<String> reminderIds;
    private String appointmentId;

    // Zamanlama
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private Boolean isPinned;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === FACTORY ===

    public static ConversationDto fromEntity(Conversation c) {
        if (c == null) return null;

        return ConversationDto.builder()
                .id(c.getId())
                .leadId(c.getLeadId())
                .patientId(c.getPatientId())
                .assignedAgentId(c.getAssignedAgentId())
                .assignedAgentName(c.getAssignedAgentName())
                .channel(c.getChannel())
                .channelDisplayName(c.getChannel() != null ? c.getChannel().getDisplayName() : null)
                .channelConversationId(c.getChannelConversationId())
                .status(c.getStatus())
                .statusDisplayName(c.getStatus() != null ? c.getStatus().getDisplayName() : null)
                .priority(c.getPriority())
                .priorityDisplayName(c.getPriority() != null ? c.getPriority().getDisplayName() : null)
                .subject(c.getSubject())
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .lastMessageSender(c.getLastMessageSender())
                .totalMessages(c.getTotalMessages())
                .unreadCount(c.getUnreadCount())
                .tags(c.getTags())
                .treatmentInterest(c.getTreatmentInterest())
                .language(c.getLanguage())
                .ticketIds(c.getTicketIds())
                .reminderIds(c.getReminderIds())
                .appointmentId(c.getAppointmentId())
                .firstResponseAt(c.getFirstResponseAt())
                .resolvedAt(c.getResolvedAt())
                .isPinned(c.getIsPinned())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    /**
     * Inbox listesi için hafif versiyon
     */
    public static ConversationDto fromEntityBasic(Conversation c) {
        if (c == null) return null;

        return ConversationDto.builder()
                .id(c.getId())
                .leadId(c.getLeadId())
                .assignedAgentId(c.getAssignedAgentId())
                .assignedAgentName(c.getAssignedAgentName())
                .channel(c.getChannel())
                .channelDisplayName(c.getChannel() != null ? c.getChannel().getDisplayName() : null)
                .status(c.getStatus())
                .statusDisplayName(c.getStatus() != null ? c.getStatus().getDisplayName() : null)
                .priority(c.getPriority())
                .subject(c.getSubject())
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .lastMessageSender(c.getLastMessageSender())
                .unreadCount(c.getUnreadCount())
                .isPinned(c.getIsPinned())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
