package com.healthvia.platform.conversation.channel;

import com.healthvia.platform.conversation.entity.Conversation;

import lombok.Builder;
import lombok.Value;

/**
 * Channel-agnostic outbound message handed to a {@link ChannelAdapter}. The
 * dispatcher fills it in from the persisted Conversation + Message + Lead.
 */
@Value
@Builder
public class OutboundMessage {

    Conversation.Channel channel;

    /** External recipient — same shape as InboundChannelMessage.externalUserId. */
    String recipient;

    /** Plain text body (channel may apply markdown / templating). */
    String text;

    /** Optional subject — used by email; ignored by Telegram. */
    String subject;

    /** Channel-specific thread ID for reply threading (email Message-ID, etc.). */
    String externalThreadId;

    /** Internal message id we just persisted, for audit logging. */
    String internalMessageId;

    /** Internal conversation id this belongs to. */
    String conversationId;
}
