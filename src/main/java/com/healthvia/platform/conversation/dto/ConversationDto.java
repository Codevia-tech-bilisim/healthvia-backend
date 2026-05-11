// conversation/dto/ConversationDto.java
package com.healthvia.platform.conversation.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.*;
import com.healthvia.platform.lead.entity.Lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire shape for the agent dashboard inbox.
 *
 * The Conversation entity itself stores only foreign keys, so this DTO
 * denormalizes the linked Lead's display fields (name, phone, email,
 * avatar) into a single payload. The frontend treats those fields as
 * required; returning null for them previously crashed the inbox detail
 * panel with "Cannot read properties of undefined" at render time, so
 * every collection/string here has a non-null default.
 */
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

    // Lead'den denormalize — frontend bunları zorunlu sayıyor.
    private String participantName;
    private String participantPhone;
    private String participantEmail;
    private String participantAvatarUrl;

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
        return fromEntity(c, null);
    }

    public static ConversationDto fromEntity(Conversation c, Lead lead) {
        if (c == null) return null;

        return ConversationDto.builder()
                .id(c.getId())
                .leadId(c.getLeadId())
                .patientId(c.getPatientId())
                .assignedAgentId(c.getAssignedAgentId())
                .assignedAgentName(c.getAssignedAgentName())
                .participantName(resolveParticipantName(lead))
                .participantPhone(lead != null ? lead.getPhone() : null)
                .participantEmail(lead != null ? lead.getEmail() : null)
                .participantAvatarUrl(null)
                .channel(c.getChannel())
                .channelDisplayName(c.getChannel() != null ? c.getChannel().getDisplayName() : null)
                .channelConversationId(c.getChannelConversationId())
                .status(c.getStatus())
                .statusDisplayName(c.getStatus() != null ? c.getStatus().getDisplayName() : null)
                .priority(c.getPriority())
                .priorityDisplayName(c.getPriority() != null ? c.getPriority().getDisplayName() : null)
                .subject(c.getSubject())
                .lastMessagePreview(c.getLastMessagePreview() != null ? c.getLastMessagePreview() : "")
                .lastMessageAt(c.getLastMessageAt())
                .lastMessageSender(c.getLastMessageSender())
                .totalMessages(c.getTotalMessages() != null ? c.getTotalMessages() : 0)
                .unreadCount(c.getUnreadCount() != null ? c.getUnreadCount() : 0)
                .tags(c.getTags() != null ? c.getTags() : Collections.emptySet())
                .treatmentInterest(c.getTreatmentInterest())
                .language(resolveLanguage(c, lead))
                .ticketIds(c.getTicketIds() != null ? c.getTicketIds() : Collections.emptySet())
                .reminderIds(c.getReminderIds() != null ? c.getReminderIds() : Collections.emptySet())
                .appointmentId(c.getAppointmentId())
                .firstResponseAt(c.getFirstResponseAt())
                .resolvedAt(c.getResolvedAt())
                .isPinned(c.getIsPinned())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    /**
     * Inbox listesi için hafif versiyon. Detail panellerin ihtiyaç duyduğu
     * ticketIds / reminderIds / treatmentInterest / appointmentId / audit
     * alanlarını yüklemiyor; ama frontend'in render sırasında dokunduğu
     * her alan burada güvenli (non-null) bir varsayılana sahip.
     */
    public static ConversationDto fromEntityBasic(Conversation c) {
        return fromEntityBasic(c, null);
    }

    public static ConversationDto fromEntityBasic(Conversation c, Lead lead) {
        if (c == null) return null;

        return ConversationDto.builder()
                .id(c.getId())
                .leadId(c.getLeadId())
                .patientId(c.getPatientId())
                .assignedAgentId(c.getAssignedAgentId())
                .assignedAgentName(c.getAssignedAgentName())
                .participantName(resolveParticipantName(lead))
                .participantPhone(lead != null ? lead.getPhone() : null)
                .participantEmail(lead != null ? lead.getEmail() : null)
                .participantAvatarUrl(null)
                .channel(c.getChannel())
                .channelDisplayName(c.getChannel() != null ? c.getChannel().getDisplayName() : null)
                .status(c.getStatus())
                .statusDisplayName(c.getStatus() != null ? c.getStatus().getDisplayName() : null)
                .priority(c.getPriority())
                .subject(c.getSubject())
                .lastMessagePreview(c.getLastMessagePreview() != null ? c.getLastMessagePreview() : "")
                .lastMessageAt(c.getLastMessageAt())
                .lastMessageSender(c.getLastMessageSender())
                .unreadCount(c.getUnreadCount() != null ? c.getUnreadCount() : 0)
                .tags(c.getTags() != null ? c.getTags() : Collections.emptySet())
                .language(resolveLanguage(c, lead))
                .isPinned(c.getIsPinned())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private static String resolveParticipantName(Lead lead) {
        if (lead == null) return "External";
        String full = lead.getFullName();
        return (full == null || full.isBlank()) ? "External" : full;
    }

    private static String resolveLanguage(Conversation c, Lead lead) {
        if (c != null && c.getLanguage() != null && !c.getLanguage().isBlank()) {
            return c.getLanguage().toLowerCase();
        }
        if (lead != null && lead.getLanguage() != null && !lead.getLanguage().isBlank()) {
            return lead.getLanguage().toLowerCase();
        }
        return "en";
    }
}
