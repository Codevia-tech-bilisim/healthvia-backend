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
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthvia.platform.conversation.entity.Conversation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Telegram Bot ⇄ HealthVia. Webhook delivery: Telegram POSTs each Update to
 * {@code /api/v1/telegram/webhook}; outbound replies hit {@code /sendMessage}.
 *
 * Disabled unless {@code telegram.channel.enabled=true} and a bot token is
 * provided. Get a token from @BotFather on Telegram.
 *
 * Required properties:
 *   telegram.channel.enabled=true
 *   telegram.bot-token=123456:AAH...
 *   telegram.webhook.url=https://api.example.com/api/v1/telegram/webhook
 *   telegram.webhook.secret-token=<random>   (recommended)
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

    @Value("${telegram.webhook.url:}")
    private String webhookUrl;

    @Value("${telegram.webhook.secret-token:}")
    private String webhookSecretToken;

    @PostConstruct
    public void registerWebhook() {
        try {
            HttpResponse<String> me = HTTP.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + botToken + "/getMe"))
                .timeout(Duration.ofSeconds(8))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (me.statusCode() == 200) {
                JsonNode bot = MAPPER.readTree(me.body()).path("result");
                log.info("✅ Telegram bot connected: @{} ({})",
                    bot.path("username").asText("unknown"),
                    bot.path("first_name").asText(""));
            } else {
                log.warn("Telegram getMe returned {}: {}", me.statusCode(), me.body());
                return;
            }
        } catch (Exception e) {
            log.warn("Telegram bot verification failed: {}", e.getMessage());
            return;
        }

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("telegram.webhook.url is empty; skipping setWebhook. "
                + "Set TELEGRAM_WEBHOOK_URL to your public POST endpoint.");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("url", webhookUrl);
            payload.put("allowed_updates", new String[] {"message"});
            payload.put("drop_pending_updates", false);
            if (webhookSecretToken != null && !webhookSecretToken.isBlank()) {
                payload.put("secret_token", webhookSecretToken);
            }
            String body = MAPPER.writeValueAsString(payload);
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + botToken + "/setWebhook"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build(), HttpResponse.BodyHandlers.ofString());
            JsonNode resp = MAPPER.readTree(r.body());
            if (r.statusCode() == 200 && resp.path("ok").asBoolean(false)) {
                log.info("✅ Telegram webhook registered: {}", webhookUrl);
            } else {
                log.error("Telegram setWebhook failed ({}): {}", r.statusCode(), r.body());
            }
        } catch (Exception e) {
            log.error("Telegram setWebhook exception: {}", e.getMessage());
        }
    }

    @Override
    public Conversation.Channel channel() {
        return Conversation.Channel.TELEGRAM;
    }

    /**
     * Called by {@link TelegramWebhookController} for each Update Telegram POSTs.
     * The Update payload mirrors the items inside getUpdates()'s result array.
     */
    public void processUpdate(JsonNode update) {
        JsonNode msg = update.path("message");
        if (msg.isMissingNode() || msg.isNull()) return;
        ingestUpdate(msg);
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
