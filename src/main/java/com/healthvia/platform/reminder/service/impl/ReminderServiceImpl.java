package com.healthvia.platform.reminder.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.conversation.service.ConversationService;
import com.healthvia.platform.reminder.entity.Reminder;
import com.healthvia.platform.reminder.entity.Reminder.*;
import com.healthvia.platform.reminder.repository.ReminderRepository;
import com.healthvia.platform.reminder.service.ReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository repository;
    private final ConversationService conversationService;

    // === CRUD ===

    @Override
    public Reminder create(Reminder reminder) {
        if (reminder.getStatus() == null) reminder.setStatus(ReminderStatus.PENDING);
        if (reminder.getPriority() == null) reminder.setPriority(ReminderPriority.NORMAL);

        // Recurring ise sonraki tekrarı hesapla
        if (reminder.getIsRecurring() && reminder.getRecurrenceRule() != null) {
            reminder.setNextOccurrence(reminder.calculateNextOccurrence());
            if (reminder.getMaxOccurrences() == null) reminder.setMaxOccurrences(10);
        }

        Reminder saved = repository.save(reminder);

        // Conversation'a link
        if (reminder.getConversationId() != null) {
            try {
                conversationService.linkReminder(reminder.getConversationId(), saved.getId());
            } catch (Exception e) {
                log.warn("Conversation link başarısız: {}", e.getMessage());
            }
        }

        log.info("Reminder created: {} type: {} remindAt: {}", saved.getId(), reminder.getType(), reminder.getRemindAt());
        return saved;
    }

    @Override
    public Reminder update(String id, Reminder updated) {
        Reminder existing = findByIdOrThrow(id);

        if (updated.getTitle() != null) existing.setTitle(updated.getTitle());
        if (updated.getNote() != null) existing.setNote(updated.getNote());
        if (updated.getType() != null) existing.setType(updated.getType());
        if (updated.getRemindAt() != null) existing.setRemindAt(updated.getRemindAt());
        if (updated.getPriority() != null) existing.setPriority(updated.getPriority());
        if (updated.getNotificationChannels() != null) existing.setNotificationChannels(updated.getNotificationChannels());

        return repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reminder> findById(String id) {
        return repository.findById(id).filter(r -> !r.isDeleted());
    }

    @Override
    public void delete(String id, String deletedBy) {
        Reminder reminder = findByIdOrThrow(id);
        reminder.markAsDeleted(deletedBy);
        repository.save(reminder);
    }

    // === SORGULAR ===

    @Override
    @Transactional(readOnly = true)
    public Page<Reminder> findByAgent(String agentId, Pageable pageable) {
        return repository.findByAssignedAgentIdAndDeletedFalseOrderByRemindAtAsc(agentId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Reminder> findByAgentAndStatus(String agentId, ReminderStatus status, Pageable pageable) {
        return repository.findByAssignedAgentIdAndStatusAndDeletedFalseOrderByRemindAtAsc(agentId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Reminder> findByStatus(ReminderStatus status, Pageable pageable) {
        return repository.findByStatusAndDeletedFalseOrderByRemindAtAsc(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Reminder> findByType(ReminderType type, Pageable pageable) {
        return repository.findByTypeAndDeletedFalseOrderByRemindAtAsc(type, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> findByConversation(String conversationId) {
        return repository.findByConversationIdAndDeletedFalseOrderByRemindAtAsc(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> findByLead(String leadId) {
        return repository.findByLeadIdAndDeletedFalseOrderByRemindAtAsc(leadId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> findByTicket(String ticketId) {
        return repository.findByTicketIdAndDeletedFalseOrderByRemindAtAsc(ticketId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> findByAppointment(String appointmentId) {
        return repository.findByAppointmentIdAndDeletedFalseOrderByRemindAtAsc(appointmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> findUpcomingByAgent(String agentId, int hoursAhead) {
        return repository.findUpcomingByAgent(agentId, LocalDateTime.now(), LocalDateTime.now().plusHours(hoursAhead));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Reminder> search(String keyword, Pageable pageable) {
        return repository.search(keyword, pageable);
    }

    // === DURUM ===

    @Override
    public Reminder complete(String id, String agentId) {
        Reminder reminder = findByIdOrThrow(id);
        reminder.setStatus(ReminderStatus.COMPLETED);
        reminder.setCompletedAt(LocalDateTime.now());
        reminder.setCompletedBy(agentId);

        // Recurring ise sonraki tekrarı oluştur
        if (reminder.getIsRecurring() && reminder.getRecurrenceRule() != null) {
            int count = reminder.getOccurrenceCount() + 1;
            reminder.setOccurrenceCount(count);

            if (reminder.getMaxOccurrences() != null && count < reminder.getMaxOccurrences()) {
                createNextRecurrence(reminder);
            }
        }

        log.info("Reminder {} completed by {}", id, agentId);
        return repository.save(reminder);
    }

    @Override
    public Reminder cancel(String id) {
        Reminder reminder = findByIdOrThrow(id);
        reminder.setStatus(ReminderStatus.CANCELLED);
        return repository.save(reminder);
    }

    @Override
    public Reminder snooze(String id, int minutes) {
        Reminder reminder = findByIdOrThrow(id);
        reminder.setStatus(ReminderStatus.SNOOZED);
        reminder.setSnoozedUntil(LocalDateTime.now().plusMinutes(minutes));
        reminder.setSnoozeCount(reminder.getSnoozeCount() + 1);
        reminder.setNotificationSent(false); // Tekrar bildirim gönderilecek

        log.info("Reminder {} snoozed for {} minutes (count: {})", id, minutes, reminder.getSnoozeCount());
        return repository.save(reminder);
    }

    // === ZAMANLAMA (@Scheduled) ===

    /**
     * Her dakika çalışır. Zamanı gelen reminder'ları TRIGGERED yapar.
     * TODO: Gerçek bildirim gönderimi (email, in-app push) NotificationService ile yapılacak.
     */
    @Override
    @Scheduled(fixedRate = 60000) // 1 dakikada bir
    public void processDueReminders() {
        List<Reminder> dueReminders = repository.findDueReminders(LocalDateTime.now());
        if (dueReminders.isEmpty()) return;

        log.info("Processing {} due reminders", dueReminders.size());

        for (Reminder reminder : dueReminders) {
            reminder.setStatus(ReminderStatus.TRIGGERED);

            // Snoozed olanı tekrar PENDING'den TRIGGERED'a al
            if (reminder.getSnoozedUntil() != null) {
                reminder.setSnoozedUntil(null);
            }

            // TODO: NotificationService.send(reminder) — Email/In-app bildirim
            reminder.setNotificationSent(true);
            reminder.setNotificationSentAt(LocalDateTime.now());

            repository.save(reminder);
            log.debug("Reminder triggered: {} for agent: {}", reminder.getId(), reminder.getAssignedAgentId());
        }
    }

    /**
     * Her saat çalışır. 24 saatten fazla TRIGGERED kalmış reminder'ları MISSED yapar.
     */
    @Override
    @Scheduled(fixedRate = 3600000) // 1 saatte bir
    public void processMissedReminders() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Reminder> missed = repository.findMissedReminders(threshold);
        if (missed.isEmpty()) return;

        log.info("Marking {} reminders as missed", missed.size());

        for (Reminder reminder : missed) {
            reminder.setStatus(ReminderStatus.MISSED);
            repository.save(reminder);
        }
    }

    /**
     * Her saat çalışır. Recurring reminder'ların sonraki tekrarını oluşturur.
     */
    @Override
    @Scheduled(fixedRate = 3600000)
    public void processRecurringReminders() {
        List<Reminder> recurringDue = repository.findRecurringDue(LocalDateTime.now());
        if (recurringDue.isEmpty()) return;

        log.info("Processing {} recurring reminders", recurringDue.size());

        for (Reminder reminder : recurringDue) {
            if (reminder.getMaxOccurrences() != null &&
                reminder.getOccurrenceCount() >= reminder.getMaxOccurrences()) {
                continue;
            }
            createNextRecurrence(reminder);
        }
    }

    // === İSTATİSTİK ===

    @Override
    @Transactional(readOnly = true)
    public long countPendingByAgent(String agentId) {
        return repository.countByAssignedAgentIdAndStatusAndDeletedFalse(agentId, ReminderStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() { return repository.countByDeletedFalse(); }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ReminderStatus status) {
        return repository.countByStatusAndDeletedFalse(status);
    }

    // === PRIVATE ===

    private Reminder findByIdOrThrow(String id) {
        return repository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", id));
    }

    /**
     * Recurring reminder'ın bir sonraki tekrarını oluştur
     */
    private void createNextRecurrence(Reminder source) {
        LocalDateTime nextTime = source.calculateNextOccurrence();
        if (nextTime == null) return;

        Reminder next = Reminder.builder()
                .conversationId(source.getConversationId())
                .leadId(source.getLeadId())
                .patientId(source.getPatientId())
                .ticketId(source.getTicketId())
                .appointmentId(source.getAppointmentId())
                .title(source.getTitle())
                .note(source.getNote())
                .type(source.getType())
                .remindAt(nextTime)
                .status(ReminderStatus.PENDING)
                .priority(source.getPriority())
                .assignedAgentId(source.getAssignedAgentId())
                .assignedAgentName(source.getAssignedAgentName())
                .createdByAgentId(source.getCreatedByAgentId())
                .createdByAgentName(source.getCreatedByAgentName())
                .isRecurring(true)
                .recurrenceRule(source.getRecurrenceRule())
                .occurrenceCount(source.getOccurrenceCount())
                .maxOccurrences(source.getMaxOccurrences())
                .notificationChannels(source.getNotificationChannels())
                .build();

        next.setNextOccurrence(next.calculateNextOccurrence());
        repository.save(next);

        log.info("Created next recurrence of reminder {} at {}", source.getId(), nextTime);
    }
}
