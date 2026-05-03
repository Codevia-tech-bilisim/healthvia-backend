package com.healthvia.platform.conversation.channel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthvia.platform.conversation.entity.Conversation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Telegram Bot ⇄ HealthVia. Long-poll {@code /getUpdates} every cycle so the
 * backend can run behind NAT without a public webhook. Each text message
 * becomes an InboundChannelMessage; outbound replies hit
 * {@code /sendMessage}.
 *
 * Disabled unless {@code telegram.channel.enabled=true} and a bot token is
 * provided. Get a token from @BotFather on Telegram.
 *
 * Required properties:
 *   telegram.channel.enabled=true
 *   telegram.bot-token=123456:AAH...
 *   telegram.poll.timeout-seconds=25
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.channel.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TelegramChannelAdapter implements ChannelAdapter {

    private static final String BASE = "https://api.telegram.org/bot";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    private final MessageInboundService inboundService;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.poll.timeout-seconds:25}")
    private int longPollTimeoutSec;

    private final AtomicLong lastUpdateId = new AtomicLong(0);

    @PostConstruct
    public void verify() {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + botToken + "/getMe"))
                .timeout(Duration.ofSeconds(8))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) {
                JsonNode body = MAPPER.readTree(r.body());
                JsonNode bot = body.path("result");
                log.info("✅ Telegram bot connected: @{} ({})",
                    bot.path("username").asText("unknown"),
                    bot.path("first_name").asText(""));
            } else {
                log.warn("Telegram getMe returned {}: {}", r.statusCode(), r.body());
            }
        } catch (Exception e) {
            log.warn("Telegram bot verification failed: {}", e.getMessage());
        }
    }

    @Override
    public Conversation.Channel channel() {
        return Conversation.Channel.TELEGRAM;
    }

    /**
     * Poll for updates. Spaced 1 s apart so we don't spam the server when
     * long-polling returns immediately (no updates).
     */
    @Scheduled(fixedDelayString = "${telegram.poll.gap-ms:1000}", initialDelayString = "${telegram.poll.initial-ms:5000}")
    public void pollUpdates() {
        try {
            String url = BASE + botToken + "/getUpdates"
                + "?timeout=" + longPollTimeoutSec
                + "&offset=" + (lastUpdateId.get() + 1);
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(longPollTimeoutSec + 5))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                log.debug("Telegram getUpdates {}: {}", r.statusCode(), r.body());
                return;
            }
            JsonNode body = MAPPER.readTree(r.body());
            if (!body.path("ok").asBoolean(false)) return;
            for (JsonNode upd : body.path("result")) {
                long updateId = upd.path("update_id").asLong();
                if (updateId > lastUpdateId.get()) lastUpdateId.set(updateId);
                JsonNode msg = upd.path("message");
                if (msg.isMissingNode() || msg.isNull()) continue;
                ingestUpdate(msg);
            }
        } catch (Exception e) {
            log.debug("Telegram poll exception: {}", e.getMessage());
        }
    }

    private void ingestUpdate(JsonNode msg) {
        try {
            String text = msg.path("text").asText("");
            if (text.isBlank()) return;
            JsonNode chat = msg.path("chat");
            JsonNode from = msg.path("from");
            String chatId = chat.path("id").asText();
            String firstName = from.path("first_name").asText("");
            String lastName = from.path("last_name").asText("");
            String username = from.path("username").asText(null);
            String langCode = from.path("language_code").asText("en");
            long unixTs = msg.path("date").asLong(Instant.now().getEpochSecond());

            InboundChannelMessage inbound = InboundChannelMessage.builder()
                .channel(Conversation.Channel.TELEGRAM)
                .externalUserId(chatId)
                .externalUserName((firstName + " " + lastName).trim().isEmpty()
                    ? (username == null ? "Telegram User" : "@" + username)
                    : (firstName + " " + lastName).trim())
                .language(langCode)
                .externalMessageId("tg-" + msg.path("message_id").asText())
                .externalThreadId(chatId)
                .text(text)
                .sentAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTs), ZoneId.systemDefault()))
                .meta(Map.of(
                    "telegramUsername", username == null ? "" : username,
                    "messageId", msg.path("message_id").asLong()))
                .build();

            inboundService.ingest(inbound);
        } catch (Exception e) {
            log.warn("Failed to ingest Telegram update: {}", e.getMessage());
        }
    }

    @Override
    public String sendText(OutboundMessage msg) {
        try {
            String payload = MAPPER.writeValueAsString(Map.of(
                "chat_id", msg.getRecipient(),
                "text", msg.getText() == null ? "" : msg.getText(),
                "parse_mode", "HTML"));
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + botToken + "/sendMessage"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build(), HttpResponse.BodyHandlers.ofString());
            JsonNode body = MAPPER.readTree(r.body());
            if (!body.path("ok").asBoolean(false)) {
                log.warn("Telegram sendMessage failed: {}", r.body());
                return null;
            }
            String mid = "tg-" + body.path("result").path("message_id").asText();
            log.info("💬 Telegram → chat={} ({} chars)", msg.getRecipient(),
                msg.getText() == null ? 0 : msg.getText().length());
            return mid;
        } catch (Exception e) {
            log.error("Telegram send failed: {}", e.getMessage());
            return null;
        }
    }
}
