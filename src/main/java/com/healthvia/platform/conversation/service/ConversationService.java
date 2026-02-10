// conversation/service/ConversationService.java
package com.healthvia.platform.conversation.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.Channel;
import com.healthvia.platform.conversation.entity.Conversation.ConversationStatus;

public interface ConversationService {

    // === CRUD ===
    Conversation create(Conversation conversation);
    Conversation update(String id, Conversation conversation);
    Optional<Conversation> findById(String id);
    void delete(String id, String deletedBy);

    // === KONUŞMA BAŞLATMA ===
    Conversation getOrCreateForLead(String leadId, Channel channel, String agentId);

    // === SORGULAR ===
    List<Conversation> findByLeadId(String leadId);
    Page<Conversation> findByAgent(String agentId, Pageable pageable);
    Page<Conversation> findByAgentAndStatus(String agentId, ConversationStatus status, Pageable pageable);
    Page<Conversation> findByStatus(ConversationStatus status, Pageable pageable);
    Page<Conversation> findByChannel(Channel channel, Pageable pageable);
    Page<Conversation> search(String keyword, Pageable pageable);

    // === DURUM ===
    Conversation changeStatus(String id, ConversationStatus newStatus);
    Conversation resolve(String id);
    Conversation archive(String id);
    Conversation reopen(String id);

    // === MESAJ HOOK ===
    Conversation onNewMessage(String conversationId, String preview, String sender);
    Conversation markAsRead(String id);

    // === ETİKET ===
    Conversation addTag(String id, String tag);
    Conversation removeTag(String id, String tag);

    // === PIN ===
    Conversation togglePin(String id);

    // === AGENT ===
    Conversation assignToAgent(String id, String agentId);
    List<Conversation> findUnreadByAgent(String agentId);
    List<Conversation> findPinnedByAgent(String agentId);

    // === İLİŞKİ ===
    Conversation linkTicket(String conversationId, String ticketId);
    Conversation linkReminder(String conversationId, String reminderId);
    Conversation linkAppointment(String conversationId, String appointmentId);

    // === İSTATİSTİK ===
    long countAll();
    long countByStatus(ConversationStatus status);
    long countByChannel(Channel channel);
    long countUnreadByAgent(String agentId);
    List<Conversation> findStaleConversations(int inactiveHours);
}
