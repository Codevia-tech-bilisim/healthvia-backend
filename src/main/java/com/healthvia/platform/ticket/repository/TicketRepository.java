package com.healthvia.platform.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.ticket.entity.Ticket;
import com.healthvia.platform.ticket.entity.Ticket.TicketCategory;
import com.healthvia.platform.ticket.entity.Ticket.TicketPriority;
import com.healthvia.platform.ticket.entity.Ticket.TicketStatus;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {

    // === KONUŞMA & LEAD BAZLI ===

    List<Ticket> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(String conversationId);

    List<Ticket> findByLeadIdAndDeletedFalseOrderByCreatedAtDesc(String leadId);

    // === AGENT BAZLI ===

    Page<Ticket> findByAssignedAgentIdAndDeletedFalseOrderByCreatedAtDesc(
            String agentId, Pageable pageable);

    Page<Ticket> findByAssignedAgentIdAndStatusAndDeletedFalseOrderByPriorityDescCreatedAtDesc(
            String agentId, TicketStatus status, Pageable pageable);

    long countByAssignedAgentIdAndStatusInAndDeletedFalse(String agentId, List<TicketStatus> statuses);

    // === DURUM BAZLI ===

    Page<Ticket> findByStatusAndDeletedFalseOrderByPriorityDescCreatedAtDesc(
            TicketStatus status, Pageable pageable);

    Page<Ticket> findByStatusInAndDeletedFalseOrderByPriorityDescCreatedAtDesc(
            List<TicketStatus> statuses, Pageable pageable);

    long countByStatusAndDeletedFalse(TicketStatus status);

    // === KATEGORİ & ÖNCELİK ===

    Page<Ticket> findByCategoryAndDeletedFalseOrderByPriorityDescCreatedAtDesc(
            TicketCategory category, Pageable pageable);

    Page<Ticket> findByPriorityAndDeletedFalseOrderByCreatedAtDesc(
            TicketPriority priority, Pageable pageable);

    Page<Ticket> findByCategoryAndStatusAndDeletedFalse(
            TicketCategory category, TicketStatus status, Pageable pageable);

    // === ATANMAMIŞ ===

    @Query("{ 'assignedAgentId': null, 'status': { $in: ['OPEN'] }, 'deleted': false }")
    List<Ticket> findUnassigned();

    // === SLA & OVERDUE ===

    @Query("{ 'slaDeadline': { $lte: ?0 }, 'status': { $in: ['OPEN','IN_PROGRESS','WAITING_CUSTOMER','WAITING_EXTERNAL'] }, 'deleted': false }")
    List<Ticket> findSlaBreachedTickets(LocalDateTime now);

    @Query("{ 'dueDate': { $lte: ?0 }, 'status': { $in: ['OPEN','IN_PROGRESS','WAITING_CUSTOMER','WAITING_EXTERNAL'] }, 'deleted': false }")
    List<Ticket> findOverdueTickets(LocalDateTime now);

    @Query("{ 'dueDate': { $gte: ?0, $lte: ?1 }, 'status': { $in: ['OPEN','IN_PROGRESS'] }, 'deleted': false }")
    List<Ticket> findTicketsDueSoon(LocalDateTime from, LocalDateTime to);

    // === ETİKET ===

    @Query("{ 'tags': ?0, 'deleted': false }")
    List<Ticket> findByTag(String tag);

    // === ARAMA ===

    @Query("{ $or: [ " +
           "{'title': {$regex: ?0, $options: 'i'}}, " +
           "{'description': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<Ticket> search(String keyword, Pageable pageable);

    // === İSTATİSTİK ===

    long countByDeletedFalse();

    long countByCategoryAndDeletedFalse(TicketCategory category);

    long countByPriorityAndStatusInAndDeletedFalse(TicketPriority priority, List<TicketStatus> statuses);

    @Query(value = "{ 'assignedAgentId': ?0, 'status': 'RESOLVED', 'deleted': false }", count = true)
    long countResolvedByAgent(String agentId);
}
