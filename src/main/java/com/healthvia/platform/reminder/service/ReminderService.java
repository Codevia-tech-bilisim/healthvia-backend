package com.healthvia.platform.reminder.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.reminder.entity.Reminder;
import com.healthvia.platform.reminder.entity.Reminder.*;

public interface ReminderService {

    // === CRUD ===
    Reminder create(Reminder reminder);
    Reminder update(String id, Reminder reminder);
    Optional<Reminder> findById(String id);
    void delete(String id, String deletedBy);

    // === SORGULAR ===
    Page<Reminder> findByAgent(String agentId, Pageable pageable);
    Page<Reminder> findByAgentAndStatus(String agentId, ReminderStatus status, Pageable pageable);
    Page<Reminder> findByStatus(ReminderStatus status, Pageable pageable);
    Page<Reminder> findByType(ReminderType type, Pageable pageable);
    List<Reminder> findByConversation(String conversationId);
    List<Reminder> findByLead(String leadId);
    List<Reminder> findByTicket(String ticketId);
    List<Reminder> findByAppointment(String appointmentId);
    List<Reminder> findUpcomingByAgent(String agentId, int hoursAhead);
    Page<Reminder> search(String keyword, Pageable pageable);

    // === DURUM ===
    Reminder complete(String id, String agentId);
    Reminder cancel(String id);
    Reminder snooze(String id, int minutes);

    // === ZAMANLAMA (@Scheduled) ===
    void processDueReminders();
    void processMissedReminders();
    void processRecurringReminders();

    // === İSTATİSTİK ===
    long countPendingByAgent(String agentId);
    long countAll();
    long countByStatus(ReminderStatus status);
}
