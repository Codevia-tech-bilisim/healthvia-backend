package com.healthvia.platform.message.controller;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.healthvia.platform.message.dto.MessageDto;
import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.service.MessageService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP üzerinden real-time mesajlaşma.
 *
 * Client tarafı kullanım:
 *
 * // Mesaj gönder
 * stompClient.send("/app/chat.send", {}, JSON.stringify({
 *     conversationId: "abc123",
 *     content: "Merhaba!",
 *     senderName: "Agent Celal"
 * }));
 *
 * // Konuşmayı dinle
 * stompClient.subscribe('/topic/conversations/abc123', (msg) => {
 *     const message = JSON.parse(msg.body);
 *     // UI'a ekle
 * });
 *
 * // Yazıyor bildirimi gönder
 * stompClient.send("/app/chat.typing", {}, JSON.stringify({
 *     conversationId: "abc123",
 *     senderName: "Agent Celal"
 * }));
 *
 * // Okundu bildirimi
 * stompClient.send("/app/chat.read", {}, JSON.stringify({
 *     conversationId: "abc123"
 * }));
 *
 * // Kendi bildirimlerini dinle
 * stompClient.subscribe('/topic/agent/{myAgentId}/notifications', (msg) => {
 *     // Notification popup göster
 * });
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    /**
     * Agent mesaj gönderir → DB'ye kaydeder → konuşmadaki herkese broadcast eder
     *
     * Client gönderir: /app/chat.send
     * Broadcast: /topic/conversations/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String agentId = principal.getName();

        // DB'ye kaydet
        Message saved = messageService.sendAgentMessage(
                request.getConversationId(),
                agentId,
                request.getSenderName(),
                request.getContent(),
                Message.MessageType.TEXT,
                null // Ek dosya WS üzerinden gönderilmez, REST ile gönderilir
        );

        // Konuşmayı dinleyenlere broadcast
        MessageDto dto = MessageDto.fromEntity(saved);
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + request.getConversationId(), dto);

        log.debug("WS message sent: conv={} agent={}", request.getConversationId(), agentId);
    }

    /**
     * "Yazıyor..." bildirimi
     *
     * Client gönderir: /app/chat.typing
     * Broadcast: /topic/conversations/{conversationId}/typing
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event, Principal principal) {
        event.setAgentId(principal.getName());
        event.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + event.getConversationId() + "/typing", event);
    }

    /**
     * Okundu bildirimi — konuşma mesajlarını okundu işaretle
     *
     * Client gönderir: /app/chat.read
     * Broadcast: /topic/conversations/{conversationId}/read
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ReadEvent event, Principal principal) {
        event.setAgentId(principal.getName());
        event.setTimestamp(LocalDateTime.now());

        // Conversation'ı okundu yap (unread sıfırla)
        // Not: ConversationService buraya inject edilemiyor çünkü MessageService zaten kullanıyor.
        // REST controller'daki markAsRead endpoint'i tercih edilebilir veya event yayınlanabilir.

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + event.getConversationId() + "/read", event);
    }

    // === HELPER: Dışarıdan çağrılabilir bildirim metodları ===

    /**
     * Belirli bir agent'a bildirim gönder.
     * Service katmanından çağrılabilir.
     */
    public void sendNotificationToAgent(String agentId, NotificationEvent notification) {
        messagingTemplate.convertAndSend(
                "/topic/agent/" + agentId + "/notifications", notification);
    }

    /**
     * Belirli bir agent'a hatırlatıcı bildirimi gönder.
     */
    public void sendReminderToAgent(String agentId, Object reminderDto) {
        messagingTemplate.convertAndSend(
                "/topic/agent/" + agentId + "/reminders", reminderDto);
    }

    /**
     * Dashboard'a real-time istatistik güncelle.
     */
    public void broadcastDashboardUpdate(Object stats) {
        messagingTemplate.convertAndSend("/topic/dashboard", stats);
    }

    /**
     * Gelen lead mesajını konuşmaya broadcast et.
     * LeadService veya webhook controller'dan çağrılır.
     */
    public void broadcastIncomingMessage(String conversationId, MessageDto messageDto) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId, messageDto);
    }

    // === REQUEST / EVENT DTOs ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageRequest {
        private String conversationId;
        private String content;
        private String senderName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypingEvent {
        private String conversationId;
        private String agentId;
        private String senderName;
        private Boolean isTyping;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadEvent {
        private String conversationId;
        private String agentId;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationEvent {
        private String type;       // "NEW_LEAD", "NEW_MESSAGE", "TICKET_ASSIGNED", "REMINDER_DUE"
        private String title;
        private String message;
        private String referenceId; // İlgili entity ID
        private String referenceType; // "LEAD", "CONVERSATION", "TICKET", "REMINDER"
        private LocalDateTime timestamp;

        public static NotificationEvent of(String type, String title, String message,
                                           String refId, String refType) {
            return new NotificationEvent(type, title, message, refId, refType, LocalDateTime.now());
        }
    }
}
