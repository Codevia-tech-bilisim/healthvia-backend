package com.healthvia.platform.ticket.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.conversation.service.ConversationService;
import com.healthvia.platform.ticket.entity.Ticket;
import com.healthvia.platform.ticket.entity.Ticket.ChecklistItem;
import com.healthvia.platform.ticket.entity.Ticket.TicketCategory;
import com.healthvia.platform.ticket.entity.Ticket.TicketPriority;
import com.healthvia.platform.ticket.entity.Ticket.TicketStatus;
import com.healthvia.platform.ticket.repository.TicketRepository;
import com.healthvia.platform.ticket.service.TicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository repository;
    private final ConversationService conversationService;

    private static final List<TicketStatus> OPEN_STATUSES = List.of(
            TicketStatus.OPEN, TicketStatus.IN_PROGRESS,
            TicketStatus.WAITING_CUSTOMER, TicketStatus.WAITING_EXTERNAL, TicketStatus.ON_HOLD
    );

    // === CRUD ===

    @Override
    public Ticket create(Ticket ticket, String agentId, String agentName) {
        if (ticket.getStatus() == null) ticket.setStatus(TicketStatus.OPEN);
        if (ticket.getPriority() == null) ticket.setPriority(TicketPriority.NORMAL);

        ticket.setReportedByAgentId(agentId);
        ticket.setReportedByAgentName(agentName);
        ticket.setStatusChangedAt(LocalDateTime.now());

        // SLA deadline hesapla
        if (ticket.getSlaDeadline() == null) {
            ticket.setSlaDeadline(LocalDateTime.now().plusHours(ticket.getPriority().getSlaHours()));
        }

        ticket.addActivity(agentId, agentName, "CREATED", "Ticket oluşturuldu: " + ticket.getTitle());

        Ticket saved = repository.save(ticket);

        // Conversation'a link
        if (ticket.getConversationId() != null) {
            try {
                conversationService.linkTicket(ticket.getConversationId(), saved.getId());
            } catch (Exception e) {
                log.warn("Conversation link başarısız: {}", e.getMessage());
            }
        }

        log.info("Ticket created: {} category: {} priority: {}", saved.getId(), ticket.getCategory(), ticket.getPriority());
        return saved;
    }

    @Override
    public Ticket update(String id, Ticket updated) {
        Ticket existing = findByIdOrThrow(id);

        if (updated.getTitle() != null) existing.setTitle(updated.getTitle());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getPriority() != null) existing.setPriority(updated.getPriority());

        return repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findById(String id) {
        return repository.findById(id).filter(t -> !t.isDeleted());
    }

    @Override
    public void delete(String id, String deletedBy) {
        Ticket ticket = findByIdOrThrow(id);
        ticket.markAsDeleted(deletedBy);
        repository.save(ticket);
    }

    // === SORGULAR ===

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findByConversation(String conversationId) {
        return repository.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findByLead(String leadId) {
        return repository.findByLeadIdAndDeletedFalseOrderByCreatedAtDesc(leadId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> findByAgent(String agentId, Pageable pageable) {
        return repository.findByAssignedAgentIdAndDeletedFalseOrderByCreatedAtDesc(agentId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> findByAgentAndStatus(String agentId, TicketStatus status, Pageable pageable) {
        return repository.findByAssignedAgentIdAndStatusAndDeletedFalseOrderByPriorityDescCreatedAtDesc(
                agentId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> findByStatus(TicketStatus status, Pageable pageable) {
        return repository.findByStatusAndDeletedFalseOrderByPriorityDescCreatedAtDesc(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> findOpenTickets(Pageable pageable) {
        return repository.findByStatusInAndDeletedFalseOrderByPriorityDescCreatedAtDesc(OPEN_STATUSES, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> findByCategory(TicketCategory category, Pageable pageable) {
        return repository.findByCategoryAndDeletedFalseOrderByPriorityDescCreatedAtDesc(category, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable) {
        return repository.findByPriorityAndDeletedFalseOrderByCreatedAtDesc(priority, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findUnassigned() {
        return repository.findUnassigned();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> search(String keyword, Pageable pageable) {
        return repository.search(keyword, pageable);
    }

    // === DURUM ===

    @Override
    public Ticket changeStatus(String id, TicketStatus newStatus, String agentId, String agentName) {
        Ticket ticket = findByIdOrThrow(id);
        TicketStatus oldStatus = ticket.getStatus();

        ticket.setPreviousStatus(oldStatus);
        ticket.setStatus(newStatus);
        ticket.setStatusChangedAt(LocalDateTime.now());

        ticket.addActivity(agentId, agentName, "STATUS_CHANGED",
                String.format("Durum: %s → %s", oldStatus.getDisplayName(), newStatus.getDisplayName()));

        log.info("Ticket {} status: {} → {}", id, oldStatus, newStatus);
        return repository.save(ticket);
    }

    @Override
    public Ticket startProgress(String id, String agentId, String agentName) {
        return changeStatus(id, TicketStatus.IN_PROGRESS, agentId, agentName);
    }

    @Override
    public Ticket resolve(String id, String resolutionNote, String agentId, String agentName) {
        Ticket ticket = changeStatus(id, TicketStatus.RESOLVED, agentId, agentName);
        ticket.setResolutionNote(resolutionNote);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setResolvedByAgentId(agentId);

        ticket.addActivity(agentId, agentName, "RESOLVED", "Çözüm: " + resolutionNote);

        return repository.save(ticket);
    }

    @Override
    public Ticket close(String id, String agentId, String agentName) {
        Ticket ticket = changeStatus(id, TicketStatus.CLOSED, agentId, agentName);
        ticket.setClosedAt(LocalDateTime.now());
        return repository.save(ticket);
    }

    @Override
    public Ticket cancel(String id, String reason, String agentId, String agentName) {
        Ticket ticket = changeStatus(id, TicketStatus.CANCELLED, agentId, agentName);
        ticket.addActivity(agentId, agentName, "CANCELLED", "İptal sebebi: " + reason);
        return repository.save(ticket);
    }

    @Override
    public Ticket reopen(String id, String agentId, String agentName) {
        return changeStatus(id, TicketStatus.OPEN, agentId, agentName);
    }

    @Override
    public Ticket putOnHold(String id, String reason, String agentId, String agentName) {
        Ticket ticket = changeStatus(id, TicketStatus.ON_HOLD, agentId, agentName);
        ticket.addActivity(agentId, agentName, "ON_HOLD", "Bekleme sebebi: " + reason);
        return repository.save(ticket);
    }

    // === ATAMA ===

    @Override
    public Ticket assignToAgent(String id, String agentId, String agentName,
                                String performedByAgentId, String performedByAgentName) {
        Ticket ticket = findByIdOrThrow(id);
        ticket.setAssignedAgentId(agentId);
        ticket.setAssignedAgentName(agentName);
        ticket.setAssignedAt(LocalDateTime.now());

        ticket.addActivity(performedByAgentId, performedByAgentName, "ASSIGNED",
                "Atanan: " + agentName);

        log.info("Ticket {} assigned to agent {}", id, agentId);
        return repository.save(ticket);
    }

    // === CHECKLIST ===

    @Override
    public Ticket addChecklistItem(String id, String text) {
        Ticket ticket = findByIdOrThrow(id);
        if (ticket.getChecklist() == null) ticket.setChecklist(new ArrayList<>());
        ticket.getChecklist().add(new ChecklistItem(text, false, null, null));
        return repository.save(ticket);
    }

    @Override
    public Ticket toggleChecklistItem(String id, int index, String agentId) {
        Ticket ticket = findByIdOrThrow(id);
        if (ticket.getChecklist() == null || index >= ticket.getChecklist().size()) {
            throw new IllegalArgumentException("Geçersiz checklist index: " + index);
        }

        ChecklistItem item = ticket.getChecklist().get(index);
        boolean newState = !item.getCompleted();
        item.setCompleted(newState);
        item.setCompletedAt(newState ? LocalDateTime.now() : null);
        item.setCompletedBy(newState ? agentId : null);

        return repository.save(ticket);
    }

    @Override
    public Ticket removeChecklistItem(String id, int index) {
        Ticket ticket = findByIdOrThrow(id);
        if (ticket.getChecklist() == null || index >= ticket.getChecklist().size()) {
            throw new IllegalArgumentException("Geçersiz checklist index: " + index);
        }
        ticket.getChecklist().remove(index);
        return repository.save(ticket);
    }

    // === ETİKET ===

    @Override
    public Ticket addTag(String id, String tag) {
        Ticket ticket = findByIdOrThrow(id);
        if (ticket.getTags() == null) ticket.setTags(new HashSet<>());
        ticket.getTags().add(tag.toUpperCase());
        return repository.save(ticket);
    }

    @Override
    public Ticket removeTag(String id, String tag) {
        Ticket ticket = findByIdOrThrow(id);
        if (ticket.getTags() != null) ticket.getTags().remove(tag.toUpperCase());
        return repository.save(ticket);
    }

    // === DUE DATE ===

    @Override
    public Ticket setDueDate(String id, LocalDateTime dueDate) {
        Ticket ticket = findByIdOrThrow(id);
        ticket.setDueDate(dueDate);
        return repository.save(ticket);
    }

    // === SLA ===

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findSlaBreachedTickets() {
        return repository.findSlaBreachedTickets(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findOverdueTickets() {
        return repository.findOverdueTickets(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findTicketsDueSoon(int hoursAhead) {
        return repository.findTicketsDueSoon(LocalDateTime.now(), LocalDateTime.now().plusHours(hoursAhead));
    }

    // === YORUM ===

    @Override
    public Ticket addComment(String id, String comment, String agentId, String agentName) {
        Ticket ticket = findByIdOrThrow(id);
        ticket.addActivity(agentId, agentName, "COMMENT", comment);
        return repository.save(ticket);
    }

    // === İSTATİSTİK ===

    @Override
    @Transactional(readOnly = true)
    public long countAll() { return repository.countByDeletedFalse(); }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(TicketStatus status) {
        return repository.countByStatusAndDeletedFalse(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpenByAgent(String agentId) {
        return repository.countByAssignedAgentIdAndStatusInAndDeletedFalse(agentId, OPEN_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public long countResolvedByAgent(String agentId) {
        return repository.countResolvedByAgent(agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatistics getStatistics() {
        TicketStatistics stats = new TicketStatistics();
        stats.setTotal(repository.countByDeletedFalse());
        stats.setOpen(repository.countByStatusAndDeletedFalse(TicketStatus.OPEN));
        stats.setInProgress(repository.countByStatusAndDeletedFalse(TicketStatus.IN_PROGRESS));
        stats.setWaitingCustomer(repository.countByStatusAndDeletedFalse(TicketStatus.WAITING_CUSTOMER));
        stats.setWaitingExternal(repository.countByStatusAndDeletedFalse(TicketStatus.WAITING_EXTERNAL));
        stats.setOnHold(repository.countByStatusAndDeletedFalse(TicketStatus.ON_HOLD));
        stats.setResolved(repository.countByStatusAndDeletedFalse(TicketStatus.RESOLVED));
        stats.setClosed(repository.countByStatusAndDeletedFalse(TicketStatus.CLOSED));
        stats.setOverdue(repository.findOverdueTickets(LocalDateTime.now()).size());
        stats.setSlaBreached(repository.findSlaBreachedTickets(LocalDateTime.now()).size());
        stats.setUnassigned(repository.findUnassigned().size());
        return stats;
    }

    // === PRIVATE ===

    private Ticket findByIdOrThrow(String id) {
        return repository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
    }
}
