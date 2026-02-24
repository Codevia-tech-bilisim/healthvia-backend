package com.healthvia.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP yapılandırması.
 *
 * Bağlantı: ws://localhost:8080/ws (SockJS fallback ile)
 *
 * Topic'ler:
 *   /topic/conversations/{conversationId}   — Konuşma mesajları (broadcast)
 *   /topic/agent/{agentId}/notifications    — Agent'a özel bildirimler
 *   /topic/agent/{agentId}/reminders        — Agent'a özel hatırlatıcılar
 *   /topic/dashboard                        — Dashboard real-time istatistik
 *
 * Queue (bireysel):
 *   /queue/messages                         — Kullanıcıya özel mesajlar
 *
 * Gönderme prefix:
 *   /app/chat.send                          — Mesaj gönder
 *   /app/chat.typing                        — Yazıyor bildirimi
 *   /app/chat.read                          — Okundu bildirimi
 *
 * Cloud Run notu:
 *   Cloud Run WebSocket bağlantılarını max 60 dakika tutar.
 *   SockJS fallback (HTTP streaming / long-polling) bu limiti aşar.
 *   Production'da min-instances=1 şart.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Server → Client: /topic (broadcast), /queue (bireysel)
        registry.enableSimpleBroker("/topic", "/queue");

        // Client → Server prefix
        registry.setApplicationDestinationPrefixes("/app");

        // Bireysel mesaj prefix (/user/{userId}/queue/...)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "https://healthviatech.website",
                        "https://www.healthviatech.website",
                        "https://admin.healthviatech.website"
                )
                .withSockJS(); // SockJS fallback — Cloud Run uyumu
    }
}
