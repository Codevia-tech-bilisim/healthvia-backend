package com.healthvia.platform.conversation.channel;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.repository.ConversationRepository;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.repository.LeadRepository;
import com.healthvia.platform.lead.service.LeadService;
import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single entry point for messages arriving from any external channel.
 *
 * Responsibilities:
 *  1. De-duplicate by externalMessageId (channel may retry).
 *  2. Find or create the Lead — by email, phone, or channel-specific id.
 *  3. Find or create the Conversation by (channel, channelConversationId).
 *  4. Persist the Message and increment Conversation counters.
 *  5. Broadcast via STOMP so any logged-in agent's UI receives the message
 *     in real time (topics already used by the inbox: /topic/conversations/{id}).
 *
 *  Lead matching strategy (in order):
 *   - existing Conversation with same channel + channelConversationId
 *   - Lead by email (exact, case-insensitive)
 *   - Lead by phone (last 9 digits to ignore country-code formatting)
 *   - Lead by externalUserId stored in tags (telegram://chatId etc.)
 *   - Otherwise: create a fresh Lead in NEW status from the inbound msg.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MessageInboundService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final LeadRepository leadRepository;
    private final LeadService leadService;   // for autoAssign
    private final SimpMessagingTemplate ws;

    public Message ingest(InboundChannelMessage in) {
        // 1. Duplicate?
        if (in.getExternalMessageId() != null) {
            Optional<Message> dup = messageRepository
                .findFirstByExternalMessageIdAndDeletedFalse(in.getExternalMessageId());
            if (dup.isPresent()) {
                log.debug("Skipping duplicate inbound msg externalId={}", in.getExternalMessageId());
                return dup.get();
            }
        }

        // 2. Conversation lookup
        Conversation conversation = findOrCreateConversation(in);
        Lead lead = leadRepository.findById(conversation.getLeadId()).orElse(null);

        // 3. Persist message
        Message msg = Message.builder()
            .conversationId(conversation.getId())
            .leadId(conversation.getLeadId())
            .senderType(Message.SenderType.LEAD)
            .senderId(conversation.getLeadId())
            .senderName(in.getExternalUserName() != null
                ? in.getExternalUserName()
                : lead != null ? (lead.getFirstName() + " " + lead.getLastName()).trim()
                              : "External")
            .messageType(Message.MessageType.TEXT)
            .content(in.getText())
            .channel(in.getChannel().name())
            .externalMessageId(in.getExternalMessageId())
            .deliveryStatus(Message.DeliveryStatus.DELIVERED)
            .deliveredAt(in.getSentAt() != null ? in.getSentAt() : LocalDateTime.now())
            .isInternalNote(false)
            .isAutoReply(false)
            .build();
        Message savedMsg = messageRepository.save(msg);

        // 4. Bump conversation counters
        conversation.setLastMessageAt(savedMsg.getDeliveredAt());
        conversation.setLastMessagePreview(safePreview(in.getText()));
        conversation.setLastMessageSender("LEAD");
        conversation.setTotalMessages((conversation.getTotalMessages() == null ? 0 : conversation.getTotalMessages()) + 1);
        conversation.setUnreadCount((conversation.getUnreadCount() == null ? 0 : conversation.getUnreadCount()) + 1);
        conversationRepository.save(conversation);

        // 5. Broadcast
        try {
            ws.convertAndSend("/topic/conversations/" + conversation.getId(), savedMsg);
            if (conversation.getAssignedAgentId() != null) {
                ws.convertAndSend("/topic/agent/" + conversation.getAssignedAgentId() + "/notifications",
                    java.util.Map.of(
                        "kind", "NEW_MESSAGE",
                        "conversationId", conversation.getId(),
                        "leadId", conversation.getLeadId(),
                        "preview", safePreview(in.getText()),
                        "channel", in.getChannel().name()));
            }
        } catch (Exception e) {
            log.warn("WS broadcast failed: {}", e.getMessage());
        }

        log.info("📨 Inbound {} → conv={} lead={} ({} chars)",
            in.getChannel(), conversation.getId(), conversation.getLeadId(),
            in.getText() == null ? 0 : in.getText().length());
        return savedMsg;
    }

    /* ---------- helpers ---------- */

    private Conversation findOrCreateConversation(InboundChannelMessage in) {
        // (channel, channelConversationId) lookup
        if (in.getExternalThreadId() != null) {
            Optional<Conversation> match = conversationRepository
                .findByChannelAndChannelConversationIdAndDeletedFalse(
                    in.getChannel(), in.getExternalThreadId());
            if (match.isPresent()) return match.get();
        }

        Lead lead = matchLead(in).orElseGet(() -> createLeadFromInbound(in));

        // Same channel, lead-scoped lookup
        Optional<Conversation> existing = conversationRepository
            .findFirstByLeadIdAndChannelAndDeletedFalseOrderByCreatedAtDesc(
                lead.getId(), in.getChannel());
        if (existing.isPresent()) {
            Conversation c = existing.get();
            if (c.getChannelConversationId() == null && in.getExternalThreadId() != null) {
                c.setChannelConversationId(in.getExternalThreadId());
                conversationRepository.save(c);
            }
            return c;
        }

        // Brand-new conversation
        Conversation c = Conversation.builder()
            .leadId(lead.getId())
            .channel(in.getChannel())
            .channelConversationId(in.getExternalThreadId())
            .subject(in.getSubject())
            .language(in.getLanguage() != null ? in.getLanguage() : lead.getLanguage())
            .status(Conversation.ConversationStatus.OPEN)
            .priority(Conversation.ConversationPriority.NORMAL)
            .totalMessages(0)
            .unreadCount(0)
            .isPinned(false)
            .build();
        return conversationRepository.save(c);
    }

    private Optional<Lead> matchLead(InboundChannelMessage in) {
        if (in.getEmail() != null && !in.getEmail().isBlank()) {
            Optional<Lead> byEmail = leadRepository.findByEmailAndDeletedFalse(in.getEmail().toLowerCase());
            if (byEmail.isPresent()) return byEmail;
        }
        if (in.getPhone() != null) {
            String tail = digitsTail(in.getPhone(), 9);
            if (!tail.isBlank()) {
                for (Lead l : leadRepository.findAll()) {
                    if (!l.isDeleted() && l.getPhone() != null
                        && digitsTail(l.getPhone(), 9).equals(tail)) {
                        return Optional.of(l);
                    }
                }
            }
        }
        // Channel-specific identifier in tags (e.g. "telegram:42389723")
        String tagKey = in.getChannel().name().toLowerCase() + ":" + in.getExternalUserId();
        for (Lead l : leadRepository.findAll()) {
            if (l.getTags() != null && l.getTags().contains(tagKey)) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    private Lead createLeadFromInbound(InboundChannelMessage in) {
        String[] nameParts = (in.getExternalUserName() != null ? in.getExternalUserName() : "External").split(" ", 2);
        Set<String> tags = new HashSet<>();
        tags.add(in.getChannel().name().toLowerCase() + ":" + in.getExternalUserId());

        Lead.LeadSource source = mapChannelToSource(in.getChannel());
        Lead lead = Lead.builder()
            .firstName(nameParts[0])
            .lastName(nameParts.length > 1 ? nameParts[1] : "")
            .email(in.getEmail())
            .phone(in.getPhone())
            .source(source)
            .status(Lead.LeadStatus.NEW)
            .priority(Lead.LeadPriority.MEDIUM)
            .language(in.getLanguage())
            .initialMessage(safePreview(in.getText()))
            .tags(tags)
            .build();
        Lead saved = leadRepository.save(lead);
        log.info("🆕 Lead auto-created from {} inbound: {} ({})", in.getChannel(), saved.getId(),
            in.getExternalUserId());

        // Auto-assign to a HealthVia employee (Lead) — language + specialty
        // match + least-busy round-robin. Failures are non-fatal: the new
        // patient stays unassigned and the admin can pick someone manually.
        try {
            Lead assigned = leadService.autoAssign(saved.getId());
            if (assigned.getAssignedAgentId() != null) {
                log.info("🤖 Auto-assigned lead {} → agent {} ({} chats)",
                    assigned.getId(), assigned.getAssignedAgentId(),
                    assigned.getStatus());
                return assigned;
            }
        } catch (Exception e) {
            log.warn("Auto-assign failed for lead {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    private static Lead.LeadSource mapChannelToSource(Conversation.Channel ch) {
        return switch (ch) {
            case WHATSAPP -> Lead.LeadSource.WHATSAPP;
            case INSTAGRAM -> Lead.LeadSource.INSTAGRAM;
            case EMAIL -> Lead.LeadSource.EMAIL;
            case PHONE -> Lead.LeadSource.PHONE;
            case SMS -> Lead.LeadSource.PHONE;
            case TELEGRAM -> Lead.LeadSource.TELEGRAM;
            case LIVE_CHAT -> Lead.LeadSource.LIVE_CHAT;
            case WEB_FORM -> Lead.LeadSource.WEB_FORM;
            case INTERNAL -> Lead.LeadSource.OTHER;
        };
    }

    private static String safePreview(String s) {
        if (s == null) return "";
        String trimmed = s.length() <= 140 ? s : s.substring(0, 139) + "…";
        return trimmed.replaceAll("\\s+", " ").trim();
    }

    private static String digitsTail(String s, int n) {
        if (s == null) return "";
        String digits = s.replaceAll("\\D", "");
        return digits.length() <= n ? digits : digits.substring(digits.length() - n);
    }
}
