// message/service/MessageService.java
package com.healthvia.platform.message.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.message.entity.Message;

public interface MessageService {

    // === GÖNDERME ===
    Message sendAgentMessage(String conversationId, String agentId, String agentName, String content,
                            Message.MessageType type, List<Message.Attachment> attachments);
    Message sendLeadMessage(String conversationId, String leadId, String leadName, String content,
                           Message.MessageType type, String channel, String externalMessageId);
    Message sendSystemMessage(String conversationId, String content);
    Message sendTemplateMessage(String conversationId, String agentId, String agentName,
                               String templateId, String templateName, String renderedContent);
    Message addInternalNote(String conversationId, String agentId, String agentName, String content);

    // === CRUD ===
    Optional<Message> findById(String id);
    Message editMessage(String id, String newContent);
    void deleteMessage(String id, String deletedBy);

    // === SORGULAR ===
    Page<Message> findByConversation(String conversationId, Pageable pageable);
    Page<Message> findVisibleByConversation(String conversationId, Pageable pageable);
    List<Message> findInternalNotes(String conversationId);
    Page<Message> searchInConversation(String conversationId, String keyword, Pageable pageable);
    Page<Message> searchGlobal(String keyword, Pageable pageable);

    // === TESLİMAT ===
    Message updateDeliveryStatus(String id, Message.DeliveryStatus status);
    Message markAsDelivered(String id);
    Message markAsRead(String id);
    Message markAsFailed(String id, String reason);
    List<Message> findFailedMessages();

    // === İSTATİSTİK ===
    long countByConversation(String conversationId);
    long countByAgent(String agentId);
    long countAll();
}
