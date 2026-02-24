package com.healthvia.platform.reminder.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.reminder.dto.ReminderDto;
import com.healthvia.platform.reminder.entity.Reminder;
import com.healthvia.platform.reminder.entity.Reminder.ReminderStatus;
import com.healthvia.platform.reminder.entity.Reminder.ReminderType;
import com.healthvia.platform.reminder.service.ReminderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reminders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    // === CRUD ===

    @PostMapping
    public ApiResponse<ReminderDto> create(@Valid @RequestBody Reminder request) {
        String agentId = SecurityUtils.getCurrentUserId();
        request.setCreatedByAgentId(agentId);
        request.setCreatedByAgentName(agentId);
        // Eğer kendine atamıyorsa, assignedAgentId request'ten gelir
        if (request.getAssignedAgentId() == null) {
            request.setAssignedAgentId(agentId);
            request.setAssignedAgentName(agentId);
        }
        Reminder created = reminderService.create(request);
        return ApiResponse.success(ReminderDto.fromEntity(created), "Hatırlatıcı oluşturuldu");
    }

    @GetMapping("/{id}")
    public ApiResponse<ReminderDto> getById(@PathVariable String id) {
        return reminderService.findById(id)
                .map(ReminderDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Hatırlatıcı bulunamadı"));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReminderDto> update(@PathVariable String id, @RequestBody Reminder request) {
        Reminder updated = reminderService.update(id, request);
        return ApiResponse.success(ReminderDto.fromEntity(updated), "Hatırlatıcı güncellendi");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        reminderService.delete(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Hatırlatıcı silindi");
    }

    // === LİSTELEME ===

    @GetMapping("/my")
    public ApiResponse<Page<ReminderDto>> getMyReminders(@PageableDefault(size = 30) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reminderService.findByAgent(agentId, pageable).map(ReminderDto::fromEntityBasic));
    }

    @GetMapping("/my/status/{status}")
    public ApiResponse<Page<ReminderDto>> getMyByStatus(
            @PathVariable ReminderStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reminderService.findByAgentAndStatus(agentId, status, pageable)
                .map(ReminderDto::fromEntityBasic));
    }

    @GetMapping("/my/upcoming")
    public ApiResponse<List<ReminderDto>> getMyUpcoming(@RequestParam(defaultValue = "24") int hours) {
        String agentId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reminderService.findUpcomingByAgent(agentId, hours)
                .stream().map(ReminderDto::fromEntityBasic).toList());
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<ReminderDto>> getByStatus(
            @PathVariable ReminderStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(reminderService.findByStatus(status, pageable).map(ReminderDto::fromEntityBasic));
    }

    @GetMapping("/type/{type}")
    public ApiResponse<Page<ReminderDto>> getByType(
            @PathVariable ReminderType type,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(reminderService.findByType(type, pageable).map(ReminderDto::fromEntityBasic));
    }

    @GetMapping("/conversation/{conversationId}")
    public ApiResponse<List<ReminderDto>> getByConversation(@PathVariable String conversationId) {
        return ApiResponse.success(reminderService.findByConversation(conversationId)
                .stream().map(ReminderDto::fromEntityBasic).toList());
    }

    @GetMapping("/lead/{leadId}")
    public ApiResponse<List<ReminderDto>> getByLead(@PathVariable String leadId) {
        return ApiResponse.success(reminderService.findByLead(leadId)
                .stream().map(ReminderDto::fromEntityBasic).toList());
    }

    @GetMapping("/ticket/{ticketId}")
    public ApiResponse<List<ReminderDto>> getByTicket(@PathVariable String ticketId) {
        return ApiResponse.success(reminderService.findByTicket(ticketId)
                .stream().map(ReminderDto::fromEntityBasic).toList());
    }

    @GetMapping("/search")
    public ApiResponse<Page<ReminderDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(reminderService.search(keyword, pageable).map(ReminderDto::fromEntityBasic));
    }

    // === DURUM İŞLEMLERİ ===

    @PatchMapping("/{id}/complete")
    public ApiResponse<ReminderDto> complete(@PathVariable String id) {
        String agentId = SecurityUtils.getCurrentUserId();
        Reminder updated = reminderService.complete(id, agentId);
        return ApiResponse.success(ReminderDto.fromEntity(updated), "Hatırlatıcı tamamlandı");
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<ReminderDto> cancel(@PathVariable String id) {
        Reminder updated = reminderService.cancel(id);
        return ApiResponse.success(ReminderDto.fromEntity(updated), "Hatırlatıcı iptal edildi");
    }

    @PatchMapping("/{id}/snooze")
    public ApiResponse<ReminderDto> snooze(
            @PathVariable String id,
            @RequestParam(defaultValue = "30") int minutes) {
        Reminder updated = reminderService.snooze(id, minutes);
        return ApiResponse.success(ReminderDto.fromEntity(updated),
                String.format("Hatırlatıcı %d dakika ertelendi", minutes));
    }

    // === İSTATİSTİK ===

    @GetMapping("/statistics")
    public ApiResponse<ReminderStatistics> getStatistics() {
        String agentId = SecurityUtils.getCurrentUserId();
        ReminderStatistics stats = new ReminderStatistics();
        stats.setTotal(reminderService.countAll());
        stats.setPending(reminderService.countByStatus(ReminderStatus.PENDING));
        stats.setTriggered(reminderService.countByStatus(ReminderStatus.TRIGGERED));
        stats.setSnoozed(reminderService.countByStatus(ReminderStatus.SNOOZED));
        stats.setCompleted(reminderService.countByStatus(ReminderStatus.COMPLETED));
        stats.setMissed(reminderService.countByStatus(ReminderStatus.MISSED));
        stats.setMyPending(reminderService.countPendingByAgent(agentId));
        return ApiResponse.success(stats);
    }

    @lombok.Data
    public static class ReminderStatistics {
        private long total;
        private long pending;
        private long triggered;
        private long snoozed;
        private long completed;
        private long missed;
        private long myPending;
    }
}
