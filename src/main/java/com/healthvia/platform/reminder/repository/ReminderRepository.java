package com.healthvia.platform.reminder.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.reminder.entity.Reminder;
import com.healthvia.platform.reminder.entity.Reminder.ReminderStatus;
import com.healthvia.platform.reminder.entity.Reminder.ReminderType;

@Repository
public interface ReminderRepository extends MongoRepository<Reminder, String> {

    // === AGENT BAZLI ===

    Page<Reminder> findByAssignedAgentIdAndDeletedFalseOrderByRemindAtAsc(
            String agentId, Pageable pageable);

    Page<Reminder> findByAssignedAgentIdAndStatusAndDeletedFalseOrderByRemindAtAsc(
            String agentId, ReminderStatus status, Pageable pageable);

    long countByAssignedAgentIdAndStatusAndDeletedFalse(String agentId, ReminderStatus status);

    // === DURUM BAZLI ===

    Page<Reminder> findByStatusAndDeletedFalseOrderByRemindAtAsc(ReminderStatus status, Pageable pageable);

    long countByStatusAndDeletedFalse(ReminderStatus status);

    // === ZAMANLAMA — @Scheduled job'lar için ===

    @Query("{ 'status': 'PENDING', 'remindAt': { $lte: ?0 }, " +
           "'$or': [{'snoozedUntil': null}, {'snoozedUntil': { $lte: ?0 }}], 'deleted': false }")
    List<Reminder> findDueReminders(LocalDateTime now);

    @Query("{ 'status': 'TRIGGERED', 'remindAt': { $lte: ?0 }, 'deleted': false }")
    List<Reminder> findMissedReminders(LocalDateTime threshold);

    // === BELİRLİ ZAMAN DİLİMİ ===

    @Query("{ 'assignedAgentId': ?0, 'status': 'PENDING', " +
           "'remindAt': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<Reminder> findUpcomingByAgent(String agentId, LocalDateTime from, LocalDateTime to);

    // === REFERANS BAZLI ===

    List<Reminder> findByConversationIdAndDeletedFalseOrderByRemindAtAsc(String conversationId);

    List<Reminder> findByLeadIdAndDeletedFalseOrderByRemindAtAsc(String leadId);

    List<Reminder> findByTicketIdAndDeletedFalseOrderByRemindAtAsc(String ticketId);

    List<Reminder> findByAppointmentIdAndDeletedFalseOrderByRemindAtAsc(String appointmentId);

    // === TİP BAZLI ===

    Page<Reminder> findByTypeAndDeletedFalseOrderByRemindAtAsc(ReminderType type, Pageable pageable);

    // === ARAMA ===

    @Query("{ $or: [ " +
           "{'title': {$regex: ?0, $options: 'i'}}, " +
           "{'note': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<Reminder> search(String keyword, Pageable pageable);

    // === RECURRING ===

    @Query("{ 'isRecurring': true, 'status': 'COMPLETED', " +
           "'nextOccurrence': { $lte: ?0 }, 'deleted': false }")
    List<Reminder> findRecurringDue(LocalDateTime now);

    // === İSTATİSTİK ===

    long countByDeletedFalse();
}
