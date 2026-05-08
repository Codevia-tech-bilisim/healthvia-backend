package com.healthvia.platform.conversation.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Receives Telegram Bot API webhook POSTs. Telegram echoes the
 * {@code secret_token} we passed to setWebhook in the
 * {@code X-Telegram-Bot-Api-Secret-Token} header; when configured we reject
 * requests that don't match.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/telegram")
@ConditionalOnProperty(name = "telegram.channel.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramChannelAdapter adapter;

    @Value("${telegram.webhook.secret-token:}")
    private String expectedSecret;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody JsonNode update) {
        if (expectedSecret != null && !expectedSecret.isBlank()
                && !expectedSecret.equals(secret)) {
            log.warn("Telegram webhook rejected: bad secret token");
            return ResponseEntity.status(403).build();
        }
        adapter.processUpdate(update);
        return ResponseEntity.ok().build();
    }
}
