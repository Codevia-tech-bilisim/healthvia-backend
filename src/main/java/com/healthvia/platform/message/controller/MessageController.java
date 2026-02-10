// message/controller/MessageController.java
package com.healthvia.platform.message.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.message.dto.MessageDto;
import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.entity.Message.*;
import com.healthvia.platform.message.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/messages")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    // === MESAJ GÖNDERME ===

    /**
     * Agent mesajı gönder
     */
    @PostMapping("/send")
    public ApiResponse<MessageDto> sendMessage(@RequestBody SendMessageRequest request) {
        String agentId = SecurityUtils.getCurrentUserId();
        // Agent adı için şimdilik ID kullanılıyor, AdminService'den çekilebilir
        Message sent = messageService.sendAgentMessage(
                request.getConversationId(),
                agentId,
                request.getSenderName(),
                request.getContent(),
                request.getMessageType(),
                request.getAttachments()
        );
        return ApiResponse.success(MessageDto.fromEntity(sent));
    }

    /**
     * Şablon mesajı gönder
     */
    @PostMapping("/send-template")
    public ApiResponse<MessageDto> sendTemplate(@RequestBody SendTemplateRequest request) {
        String agentId = SecurityUtils.getCurrentUserId();
        Message sent = messageService.sendTemplateMessage(
                request.getConversationId(),
                agentId,
                request.getSenderName(),
                request.getTemplateId(),
                request.getTemplateName(),
                request.getRenderedContent()
        );
        return ApiResponse.success(MessageDto.fromEntity(sent));
    }

    /**
     * Dahili not ekle
     */
    @PostMapping("/internal-note")
    public ApiResponse<MessageDto> addInternalNote(@RequestBody InternalNoteRequest request) {
        String agentId = SecurityUtils.getCurrentUserId();
        Message note = messageService.addInternalNote(
                request.getConversationId(),
                agentId,
                request.getSenderName(),
                request.getContent()
        );
        return ApiResponse.success(MessageDto.fromEntity(note), "Not eklendi");
    }

    // === WEBHOOK — Dış kanallardan gelen mesajlar ===

    /**
     * WhatsApp/Instagram/Email'den gelen mesaj (webhook)
     */
    @PostMapping("/webhook/incoming")
    public ApiResponse<MessageDto> incomingMessage(@RequestBody IncomingMessageRequest request) {
        Message received = messageService.sendLeadMessage(
                request.getConversationId(),
                request.getLeadId(),
                request.getLeadName(),
                request.getContent(),
                request.getMessageType(),
                request.getChannel(),
                request.getExternalMessageId()
        );
        return ApiResponse.success(MessageDto.fromEntity(received));
    }

    // === SORGULAR ===

    /**
     * Konuşmadaki mesajlar (tümü — agentler için)
     */
    @GetMapping("/conversation/{conversationId}")
    public ApiResponse<Page<MessageDto>> getByConversation(
            @PathVariable String conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<Message> messages = messageService.findByConversation(conversationId, pageable);
        return ApiResponse.success(messages.map(MessageDto::fromEntity));
    }

    /**
     * Konuşmadaki dahili notlar
     */
    @GetMapping("/conversation/{conversationId}/notes")
    public ApiResponse<List<MessageDto>> getInternalNotes(@PathVariable String conversationId) {
        List<Message> notes = messageService.findInternalNotes(conversationId);
        return ApiResponse.success(notes.stream().map(MessageDto::fromEntity).toList());
    }

    /**
     * Konuşma içi arama
     */
    @GetMapping("/conversation/{conversationId}/search")
    public ApiResponse<Page<MessageDto>> searchInConversation(
            @PathVariable String conversationId,
            @RequestParam String keyword,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<Message> messages = messageService.searchInConversation(conversationId, keyword, pageable);
        return ApiResponse.success(messages.map(MessageDto::fromEntity));
    }

    /**
     * Global arama
     */
    @GetMapping("/search")
    public ApiResponse<Page<MessageDto>> searchGlobal(
            @RequestParam String keyword,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<Message> messages = messageService.searchGlobal(keyword, pageable);
        return ApiResponse.success(messages.map(MessageDto::fromEntity));
    }

    // === DÜZENLEME & SİLME ===

    @PatchMapping("/{id}/edit")
    public ApiResponse<MessageDto> editMessage(
            @PathVariable String id,
            @RequestParam String newContent) {
        Message updated = messageService.editMessage(id, newContent);
        return ApiResponse.success(MessageDto.fromEntity(updated), "Mesaj düzenlendi");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMessage(@PathVariable String id) {
        messageService.deleteMessage(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Mesaj silindi");
    }

    // === TESLİMAT DURUMU ===

    @PatchMapping("/{id}/delivered")
    public ApiResponse<MessageDto> markDelivered(@PathVariable String id) {
        Message updated = messageService.markAsDelivered(id);
        return ApiResponse.success(MessageDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<MessageDto> markRead(@PathVariable String id) {
        Message updated = messageService.markAsRead(id);
        return ApiResponse.success(MessageDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/failed")
    public ApiResponse<MessageDto> markFailed(
            @PathVariable String id,
            @RequestParam String reason) {
        Message updated = messageService.markAsFailed(id, reason);
        return ApiResponse.success(MessageDto.fromEntity(updated));
    }

    // === REQUEST DTOs ===

    @lombok.Data
    public static class SendMessageRequest {
        private String conversationId;
        private String senderName;
        private String content;
        private MessageType messageType;
        private List<Attachment> attachments;
    }

    @lombok.Data
    public static class SendTemplateRequest {
        private String conversationId;
        private String senderName;
        private String templateId;
        private String templateName;
        private String renderedContent;
    }

    @lombok.Data
    public static class InternalNoteRequest {
        private String conversationId;
        private String senderName;
        private String content;
    }

    @lombok.Data
    public static class IncomingMessageRequest {
        private String conversationId;
        private String leadId;
        private String leadName;
        private String content;
        private MessageType messageType;
        private String channel;
        private String externalMessageId;
    }
}
