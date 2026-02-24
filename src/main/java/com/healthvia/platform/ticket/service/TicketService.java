package com.healthvia.platform.ticket.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.ticket.entity.Ticket;
import com.healthvia.platform.ticket.entity.Ticket.*;

public interface TicketService {

    // === CRUD ===
    Ticket create(Ticket ticket, String agentId, String agentName);
    Ticket update(String id, Ticket ticket);
    Optional<Ticket> findById(String id);
    void delete(String id, String deletedBy);

    // === SORGULAR ===
    List<Ticket> findByConversation(String conversationId);
    List<Ticket> findByLead(String leadId);
    Page<Ticket> findByAgent(String agentId, Pageable pageable);
    Page<Ticket> findByAgentAndStatus(String agentId, TicketStatus status, Pageable pageable);
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findOpenTickets(Pageable pageable);
    Page<Ticket> findByCategory(TicketCategory category, Pageable pageable);
    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);
    List<Ticket> findUnassigned();
    Page<Ticket> search(String keyword, Pageable pageable);

    // === DURUM ===
    Ticket changeStatus(String id, TicketStatus newStatus, String agentId, String agentName);
    Ticket startProgress(String id, String agentId, String agentName);
    Ticket resolve(String id, String resolutionNote, String agentId, String agentName);
    Ticket close(String id, String agentId, String agentName);
    Ticket cancel(String id, String reason, String agentId, String agentName);
    Ticket reopen(String id, String agentId, String agentName);
    Ticket putOnHold(String id, String reason, String agentId, String agentName);

    // === ATAMA ===
    Ticket assignToAgent(String id, String agentId, String agentName, String performedByAgentId, String performedByAgentName);

    // === CHECKLIST ===
    Ticket addChecklistItem(String id, String text);
    Ticket toggleChecklistItem(String id, int index, String agentId);
    Ticket removeChecklistItem(String id, int index);

    // === ETİKET ===
    Ticket addTag(String id, String tag);
    Ticket removeTag(String id, String tag);

    // === DUE DATE ===
    Ticket setDueDate(String id, LocalDateTime dueDate);

    // === SLA ===
    List<Ticket> findSlaBreachedTickets();
    List<Ticket> findOverdueTickets();
    List<Ticket> findTicketsDueSoon(int hoursAhead);

    // === YORUM ===
    Ticket addComment(String id, String comment, String agentId, String agentName);

    // === İSTATİSTİK ===
    long countAll();
    long countByStatus(TicketStatus status);
    long countOpenByAgent(String agentId);
    long countResolvedByAgent(String agentId);
    TicketStatistics getStatistics();

    // === İSTATİSTİK DTO ===
    @lombok.Data
    class TicketStatistics {
        private long total;
        private long open;
        private long inProgress;
        private long waitingCustomer;
        private long waitingExternal;
        private long onHold;
        private long resolved;
        private long closed;
        private long overdue;
        private long slaBreached;
        private long unassigned;
    }
}
