// message/repository/MessageRepository.java
package com.healthvia.platform.message.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.entity.Message.DeliveryStatus;
import com.healthvia.platform.message.entity.Message.SenderType;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // === KONUŞMA BAZLI ===

    Page<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(
            String conversationId, Pageable pageable);

    Page<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
            String conversationId, Pageable pageable);

    List<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(String conversationId);

    long countByConversationIdAndDeletedFalse(String conversationId);

    // Dahili notlar hariç (lead'e gösterilecek mesajlar)
    @Query("{ 'conversationId': ?0, 'isInternalNote': { $ne: true }, 'deleted': false }")
    Page<Message> findVisibleByConversation(String conversationId, Pageable pageable);

    // Sadece dahili notlar
    @Query("{ 'conversationId': ?0, 'isInternalNote': true, 'deleted': false }")
    List<Message> findInternalNotesByConversation(String conversationId);

    // === GÖNDERİCİ BAZLI ===

    Page<Message> findBySenderIdAndDeletedFalseOrderByCreatedAtDesc(
            String senderId, Pageable pageable);

    long countBySenderIdAndSenderTypeAndDeletedFalse(String senderId, SenderType senderType);

    // === TESLİMAT DURUMU ===

    @Query("{ 'conversationId': ?0, 'deliveryStatus': ?1, 'deleted': false }")
    List<Message> findByConversationAndDeliveryStatus(String conversationId, DeliveryStatus status);

    @Query("{ 'deliveryStatus': 'FAILED', 'deleted': false }")
    List<Message> findFailedMessages();

    @Query("{ 'deliveryStatus': 'PENDING', 'createdAt': { $lte: ?0 }, 'deleted': false }")
    List<Message> findStalePendingMessages(LocalDateTime threshold);

    // === ARAMA ===

    @Query("{ 'conversationId': ?0, 'content': { $regex: ?1, $options: 'i' }, 'deleted': false }")
    Page<Message> searchInConversation(String conversationId, String keyword, Pageable pageable);

    @Query("{ 'content': { $regex: ?0, $options: 'i' }, 'deleted': false }")
    Page<Message> searchGlobal(String keyword, Pageable pageable);

    // === ŞABLON BAZLI ===

    long countByTemplateIdAndDeletedFalse(String templateId);

    // === ZAMAN BAZLI ===

    Page<Message> findByConversationIdAndCreatedAtBetweenAndDeletedFalse(
            String conversationId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByCreatedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);

    // === İSTATİSTİK ===

    long countByDeletedFalse();

    @Query(value = "{ 'senderType': 'AGENT', 'senderId': ?0, 'deleted': false }", count = true)
    long countByAgent(String agentId);

    /** De-duplication for inbound channel adapters. */
    java.util.Optional<Message> findFirstByExternalMessageIdAndDeletedFalse(String externalMessageId);
}
