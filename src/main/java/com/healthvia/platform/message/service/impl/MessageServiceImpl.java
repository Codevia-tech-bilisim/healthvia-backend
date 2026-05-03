// message/service/impl/MessageServiceImpl.java
package com.healthvia.platform.message.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.conversation.channel.ChannelDispatcher;
import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.repository.ConversationRepository;
import com.healthvia.platform.conversation.service.ConversationService;
import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.entity.Message.*;
import com.healthvia.platform.message.repository.MessageRepository;
import com.healthvia.platform.message.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final ChannelDispatcher channelDispatcher;

    // === GÖNDERME ===

    @Override
    public Message sendAgentMessage(String conversationId, String agentId, String agentName,
                                   String content, MessageType type, List<Attachment> attachments) {

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.AGENT)
                .senderId(agentId)
                .senderName(agentName)
                .messageType(type != null ? type : MessageType.TEXT)
                .content(content)
                .attachments(attachments)
                .deliveryStatus(DeliveryStatus.SENT)
                .isInternalNote(false)
                .isAutoReply(false)
                .build();

        Message saved = messageRepository.save(message);

        // Konuşmayı güncelle
        conversationService.onNewMessage(conversationId, saved.getPreview(), "AGENT");

        // Outbound dispatch — sadece dış kanal (EMAIL/TELEGRAM/WHATSAPP/SMS) için
        try {
            Conversation conv = conversationRepository.findById(conversationId).orElse(null);
            if (conv != null && conv.getChannel() != null
                && conv.getChannel() != Conversation.Channel.INTERNAL
                && conv.getChannel() != Conversation.Channel.LIVE_CHAT
                && conv.getChannel() != Conversation.Channel.WEB_FORM) {
                channelDispatcher.dispatch(conv, saved.getId(), content, conv.getSubject())
                    .ifPresent(extId -> {
                        saved.setExternalMessageId(extId);
                        saved.setDeliveryStatus(DeliveryStatus.DELIVERED);
                        messageRepository.save(saved);
                    });
            }
        } catch (Exception e) {
            log.warn("Outbound channel dispatch failed for msg {}: {}", saved.getId(), e.getMessage());
        }

        log.info("Agent message sent in conversation: {}", conversationId);
        return saved;
    }

    @Override
    public Message sendLeadMessage(String conversationId, String leadId, String leadName,
                                  String content, MessageType type, String channel,
                                  String externalMessageId) {

        Message message = Message.builder()
                .conversationId(conversationId)
                .leadId(leadId)
                .senderType(SenderType.LEAD)
                .senderId(leadId)
                .senderName(leadName)
                .messageType(type != null ? type : MessageType.TEXT)
                .content(content)
                .channel(channel)
                .externalMessageId(externalMessageId)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .isInternalNote(false)
                .build();

        Message saved = messageRepository.save(message);

        // Konuşmayı güncelle
        conversationService.onNewMessage(conversationId, saved.getPreview(), "LEAD");

        log.info("Lead message received in conversation: {}", conversationId);
        return saved;
    }

    @Override
    public Message sendSystemMessage(String conversationId, String content) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.SYSTEM)
                .senderName("Sistem")
                .messageType(MessageType.SYSTEM_EVENT)
                .content(content)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .isInternalNote(false)
                .build();

        return messageRepository.save(message);
    }

    @Override
    public Message sendTemplateMessage(String conversationId, String agentId, String agentName,
                                      String templateId, String templateName, String renderedContent) {

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.AGENT)
                .senderId(agentId)
                .senderName(agentName)
                .messageType(MessageType.TEMPLATE)
                .content(renderedContent)
                .templateId(templateId)
                .templateName(templateName)
                .deliveryStatus(DeliveryStatus.SENT)
                .isInternalNote(false)
                .isAutoReply(false)
                .build();

        Message saved = messageRepository.save(message);
        conversationService.onNewMessage(conversationId, saved.getPreview(), "AGENT");

        log.info("Template message sent: {} in conversation: {}", templateName, conversationId);
        return saved;
    }

    @Override
    public Message addInternalNote(String conversationId, String agentId, String agentName, String content) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.AGENT)
                .senderId(agentId)
                .senderName(agentName)
                .messageType(MessageType.INTERNAL_NOTE)
                .content(content)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .isInternalNote(true)
                .build();

        log.info("Internal note added in conversation: {} by agent: {}", conversationId, agentId);
        return messageRepository.save(message);
    }

    // === CRUD ===

    @Override
    @Transactional(readOnly = true)
    public Optional<Message> findById(String id) {
        return messageRepository.findById(id).filter(m -> !m.isDeleted());
    }

    @Override
    public Message editMessage(String id, String newContent) {
        Message message = findByIdOrThrow(id);

        if (message.getOriginalContent() == null) {
            message.setOriginalContent(message.getContent());
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        return messageRepository.save(message);
    }

    @Override
    public void deleteMessage(String id, String deletedBy) {
        Message message = findByIdOrThrow(id);
        message.markAsDeleted(deletedBy);
        messageRepository.save(message);
    }

    // === SORGULAR ===

    @Override
    @Transactional(readOnly = true)
    public Page<Message> findByConversation(String conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(
                conversationId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> findVisibleByConversation(String conversationId, Pageable pageable) {
        return messageRepository.findVisibleByConversation(conversationId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findInternalNotes(String conversationId) {
        return messageRepository.findInternalNotesByConversation(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> searchInConversation(String conversationId, String keyword, Pageable pageable) {
        return messageRepository.searchInConversation(conversationId, keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> searchGlobal(String keyword, Pageable pageable) {
        return messageRepository.searchGlobal(keyword, pageable);
    }

    // === TESLİMAT ===

    @Override
    public Message updateDeliveryStatus(String id, DeliveryStatus status) {
        Message message = findByIdOrThrow(id);
        message.setDeliveryStatus(status);

        if (status == DeliveryStatus.DELIVERED) {
            message.setDeliveredAt(LocalDateTime.now());
        } else if (status == DeliveryStatus.READ) {
            message.setReadAt(LocalDateTime.now());
        }

        return messageRepository.save(message);
    }

    @Override
    public Message markAsDelivered(String id) {
        return updateDeliveryStatus(id, DeliveryStatus.DELIVERED);
    }

    @Override
    public Message markAsRead(String id) {
        return updateDeliveryStatus(id, DeliveryStatus.READ);
    }

    @Override
    public Message markAsFailed(String id, String reason) {
        Message message = findByIdOrThrow(id);
        message.setDeliveryStatus(DeliveryStatus.FAILED);
        message.setFailedReason(reason);
        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findFailedMessages() {
        return messageRepository.findFailedMessages();
    }

    // === İSTATİSTİK ===

    @Override
    @Transactional(readOnly = true)
    public long countByConversation(String conversationId) {
        return messageRepository.countByConversationIdAndDeletedFalse(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAgent(String agentId) {
        return messageRepository.countByAgent(agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return messageRepository.countByDeletedFalse();
    }

    // === PRIVATE ===

    private Message findByIdOrThrow(String id) {
        return messageRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));
    }
}
