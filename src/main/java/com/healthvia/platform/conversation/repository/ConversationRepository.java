// conversation/repository/ConversationRepository.java
package com.healthvia.platform.conversation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.Channel;
import com.healthvia.platform.conversation.entity.Conversation.ConversationStatus;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // === LEAD & PATIENT BAZLI ===

    List<Conversation> findByLeadIdAndDeletedFalseOrderByLastMessageAtDesc(String leadId);

    Optional<Conversation> findByLeadIdAndChannelAndStatusNotAndDeletedFalse(
            String leadId, Channel channel, ConversationStatus excludeStatus);

    Optional<Conversation> findByPatientIdAndDeletedFalse(String patientId);

    // === AGENT BAZLI ===

    Page<Conversation> findByAssignedAgentIdAndDeletedFalseOrderByLastMessageAtDesc(
            String agentId, Pageable pageable);

    Page<Conversation> findByAssignedAgentIdAndStatusAndDeletedFalseOrderByLastMessageAtDesc(
            String agentId, ConversationStatus status, Pageable pageable);

    long countByAssignedAgentIdAndStatusAndDeletedFalse(String agentId, ConversationStatus status);

    // === DURUM BAZLI ===

    Page<Conversation> findByStatusAndDeletedFalseOrderByLastMessageAtDesc(
            ConversationStatus status, Pageable pageable);

    List<Conversation> findByStatusInAndDeletedFalseOrderByLastMessageAtDesc(
            List<ConversationStatus> statuses);

    long countByStatusAndDeletedFalse(ConversationStatus status);

    // === KANAL BAZLI ===

    Page<Conversation> findByChannelAndDeletedFalse(Channel channel, Pageable pageable);

    Optional<Conversation> findByChannelConversationIdAndDeletedFalse(String channelConversationId);

    // === OKUNMAMIŞ ===

    @Query("{ 'assignedAgentId': ?0, 'unreadCount': { $gt: 0 }, 'deleted': false }")
    List<Conversation> findUnreadByAgent(String agentId);

    @Query("{ 'unreadCount': { $gt: 0 }, 'deleted': false }")
    long countTotalUnread();

    // === SABİTLENMİŞ ===

    List<Conversation> findByAssignedAgentIdAndIsPinnedTrueAndDeletedFalse(String agentId);

    // === ARAMA ===

    @Query("{ $or: [ " +
           "{'subject': {$regex: ?0, $options: 'i'}}, " +
           "{'lastMessagePreview': {$regex: ?0, $options: 'i'}}, " +
           "{'tags': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<Conversation> search(String keyword, Pageable pageable);

    // === ETİKET ===

    @Query("{ 'tags': ?0, 'deleted': false }")
    List<Conversation> findByTag(String tag);

    // === ZAMAN BAZLI ===

    @Query("{ 'lastMessageAt': { $lte: ?0 }, 'status': { $in: ['OPEN','WAITING_REPLY','AGENT_REPLY'] }, 'deleted': false }")
    List<Conversation> findStaleConversations(LocalDateTime threshold);

    // === İSTATİSTİK ===

    long countByDeletedFalse();

    long countByChannelAndDeletedFalse(Channel channel);

    // === EXTERNAL CHANNEL LOOKUP ===

    java.util.Optional<Conversation> findByChannelAndChannelConversationIdAndDeletedFalse(
        Channel channel, String channelConversationId);

    java.util.Optional<Conversation> findFirstByLeadIdAndChannelAndDeletedFalseOrderByCreatedAtDesc(
        String leadId, Channel channel);
}
