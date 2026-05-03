package com.healthvia.platform.conversation.channel;

import com.healthvia.platform.conversation.entity.Conversation;

/**
 * Pluggable transport for one external messaging channel (Email, Telegram,
 * WhatsApp Cloud, Instagram Graph, …). Each channel has its own polling /
 * webhook ingestion path but they all converge on the same internal
 * Conversation + Message data model via {@link InboundChannelMessage}.
 *
 * Implementations register themselves as Spring beans; the dispatcher
 * resolves the right adapter by {@link Conversation.Channel}.
 */
public interface ChannelAdapter {

    /** Which channel this adapter handles. */
    Conversation.Channel channel();

    /**
     * Send an outbound text message. Implementations are responsible for
     * delivering to the patient — the message is already persisted on our
     * side, return value is the channel-specific external id (for audit
     * / threading) or null if not applicable.
     *
     * @return external message id from the channel, or null
     */
    String sendText(OutboundMessage message);

    /**
     * Lifecycle hook so the adapter can start any background resources
     * (IMAP poller, Telegram long-poll loop, etc.). Called automatically
     * after Spring boots — see the per-implementation @PostConstruct.
     */
    default void start() {}

    /** Symmetric shutdown hook. */
    default void stop() {}
}
