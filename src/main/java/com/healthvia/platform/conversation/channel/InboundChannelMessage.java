package com.healthvia.platform.conversation.channel;

import java.time.LocalDateTime;
import java.util.Map;

import com.healthvia.platform.conversation.entity.Conversation;

import lombok.Builder;
import lombok.Value;

/**
 * Channel-agnostic shape of an incoming message. The MessageInboundService
 * normalises any platform's payload (raw email, Telegram update, WhatsApp
 * webhook) into this struct, then routes it to the right Conversation /
 * creates a new Lead if there's no match.
 */
@Value
@Builder
public class InboundChannelMessage {

    /** WhatsApp / TELEGRAM / EMAIL / etc. */
    Conversation.Channel channel;

    /** Stable identifier of the patient on that channel.
     *  - Email: from address (lowercased)
     *  - Telegram: chatId as string
     *  - WhatsApp: E.164 phone
     */
    String externalUserId;

    /** Display name as the channel sees the patient. */
    String externalUserName;

    /** Optional contact extras (email, phone). */
    String email;
    String phone;

    /** Optional language hint from the channel ("en", "tr", "ar", …). */
    String language;

    /** External message id within the channel (for de-duplication). */
    String externalMessageId;

    /** External thread/conversation id (email Message-ID In-Reply-To, Telegram chat). */
    String externalThreadId;

    /** Plain text body. */
    String text;

    /** Subject line (email) or null. */
    String subject;

    /** When the channel says the message was sent. */
    LocalDateTime sentAt;

    /** Free-form metadata for audit / debugging (raw payload digest, headers, …). */
    Map<String, Object> meta;
}
