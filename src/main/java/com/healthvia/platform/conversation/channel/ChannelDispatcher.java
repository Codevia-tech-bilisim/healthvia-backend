package com.healthvia.platform.conversation.channel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.repository.LeadRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Routes outbound messages to the right {@link ChannelAdapter} based on the
 * conversation's channel. If no adapter is registered (e.g. Telegram disabled
 * because the bot token is missing), the dispatcher logs and returns null —
 * the message stays persisted on our side without being delivered, which is
 * fine for the in-app inbox demo.
 *
 * Resolution order for the recipient address:
 *   - For EMAIL: lead.email
 *   - For TELEGRAM: tag "telegram:<chatId>" on the lead
 *   - For WHATSAPP / SMS / PHONE: lead.phone
 *   - For INSTAGRAM: lead.instagramHandle
 *   - For LIVE_CHAT / WEB_FORM / INTERNAL: nothing — internal-only.
 */
@Slf4j
@Component
public class ChannelDispatcher {

    private final Map<Conversation.Channel, ChannelAdapter> adapters = new EnumMap<>(Conversation.Channel.class);
    private final LeadRepository leadRepository;

    public ChannelDispatcher(List<ChannelAdapter> all, LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
        for (ChannelAdapter a : all) {
            adapters.put(a.channel(), a);
            log.info("ChannelDispatcher registered adapter for {}", a.channel());
        }
    }

    /**
     * Try to deliver. Returns the external message id from the channel (for
     * audit) or empty if the channel has no adapter / failed.
     */
    public Optional<String> dispatch(
            Conversation conversation,
            String internalMessageId,
            String text,
            String subject) {

        Conversation.Channel ch = conversation.getChannel();
        ChannelAdapter adapter = adapters.get(ch);
        if (adapter == null) {
            log.debug("No adapter registered for channel {} — message stays in-app only", ch);
            return Optional.empty();
        }

        String recipient = resolveRecipient(conversation);
        if (recipient == null) {
            log.warn("No recipient address for conv={} channel={}", conversation.getId(), ch);
            return Optional.empty();
        }

        OutboundMessage out = OutboundMessage.builder()
            .channel(ch)
            .recipient(recipient)
            .text(text)
            .subject(subject != null ? subject : conversation.getSubject())
            .externalThreadId(conversation.getChannelConversationId())
            .internalMessageId(internalMessageId)
            .conversationId(conversation.getId())
            .build();

        String externalId = adapter.sendText(out);
        return Optional.ofNullable(externalId);
    }

    /** Lookup the channel-specific external address from the conversation's lead. */
    private String resolveRecipient(Conversation c) {
        if (c.getLeadId() == null) return null;
        Lead lead = leadRepository.findById(c.getLeadId()).orElse(null);
        if (lead == null) return null;

        return switch (c.getChannel()) {
            case EMAIL -> lead.getEmail();
            case TELEGRAM -> findTagValue(lead, "telegram:");
            case WHATSAPP -> lead.getWhatsappNumber() != null ? lead.getWhatsappNumber() : lead.getPhone();
            case SMS, PHONE -> lead.getPhone();
            case INSTAGRAM -> lead.getInstagramHandle();
            default -> null;
        };
    }

    private static String findTagValue(Lead lead, String prefix) {
        if (lead.getTags() == null) return null;
        for (String t : lead.getTags()) {
            if (t != null && t.startsWith(prefix)) return t.substring(prefix.length());
        }
        return null;
    }
}
