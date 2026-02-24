package com.healthvia.platform.ticket.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.ticket.dto.TicketDto;
import com.healthvia.platform.ticket.entity.Ticket;
import com.healthvia.platform.ticket.entity.Ticket.*;
import com.healthvia.platform.ticket.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // === CRUD ===

    @PostMapping
    public ApiResponse<TicketDto> create(@Valid @RequestBody Ticket request) {
        String agentId = SecurityUtils.getCurrentUserId();
        // TODO: agent name SecurityContext veya AdminService'den çekilecek
        Ticket created = ticketService.create(request, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(created), "Ticket oluşturuldu");
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketDto> getById(@PathVariable String id) {
        return ticketService.findById(id)
                .map(TicketDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Ticket bulunamadı"));
    }

    @PutMapping("/{id}")
    public ApiResponse<TicketDto> update(@PathVariable String id, @RequestBody Ticket request) {
        Ticket updated = ticketService.update(id, request);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket güncellendi");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        ticketService.delete(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Ticket silindi");
    }

    // === LİSTELEME ===

    @GetMapping("/my")
    public ApiResponse<Page<TicketDto>> getMyTickets(@PageableDefault(size = 20) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(ticketService.findByAgent(agentId, pageable).map(TicketDto::fromEntityBasic));
    }

    @GetMapping("/my/status/{status}")
    public ApiResponse<Page<TicketDto>> getMyByStatus(
            @PathVariable TicketStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(ticketService.findByAgentAndStatus(agentId, status, pageable)
                .map(TicketDto::fromEntityBasic));
    }

    @GetMapping("/open")
    public ApiResponse<Page<TicketDto>> getOpenTickets(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(ticketService.findOpenTickets(pageable).map(TicketDto::fromEntityBasic));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<TicketDto>> getByStatus(
            @PathVariable TicketStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(ticketService.findByStatus(status, pageable).map(TicketDto::fromEntityBasic));
    }

    @GetMapping("/category/{category}")
    public ApiResponse<Page<TicketDto>> getByCategory(
            @PathVariable TicketCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(ticketService.findByCategory(category, pageable).map(TicketDto::fromEntityBasic));
    }

    @GetMapping("/priority/{priority}")
    public ApiResponse<Page<TicketDto>> getByPriority(
            @PathVariable TicketPriority priority,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(ticketService.findByPriority(priority, pageable).map(TicketDto::fromEntityBasic));
    }

    @GetMapping("/conversation/{conversationId}")
    public ApiResponse<List<TicketDto>> getByConversation(@PathVariable String conversationId) {
        return ApiResponse.success(ticketService.findByConversation(conversationId)
                .stream().map(TicketDto::fromEntityBasic).toList());
    }

    @GetMapping("/lead/{leadId}")
    public ApiResponse<List<TicketDto>> getByLead(@PathVariable String leadId) {
        return ApiResponse.success(ticketService.findByLead(leadId)
                .stream().map(TicketDto::fromEntityBasic).toList());
    }

    @GetMapping("/unassigned")
    public ApiResponse<List<TicketDto>> getUnassigned() {
        return ApiResponse.success(ticketService.findUnassigned()
                .stream().map(TicketDto::fromEntityBasic).toList());
    }

    @GetMapping("/search")
    public ApiResponse<Page<TicketDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(ticketService.search(keyword, pageable).map(TicketDto::fromEntityBasic));
    }

    // === DURUM İŞLEMLERİ ===

    @PatchMapping("/{id}/status")
    public ApiResponse<TicketDto> changeStatus(
            @PathVariable String id, @RequestParam TicketStatus status) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.changeStatus(id, status, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Durum güncellendi");
    }

    @PatchMapping("/{id}/start")
    public ApiResponse<TicketDto> startProgress(@PathVariable String id) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.startProgress(id, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "İşleme alındı");
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<TicketDto> resolve(
            @PathVariable String id, @RequestParam String resolutionNote) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.resolve(id, resolutionNote, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket çözüldü");
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<TicketDto> close(@PathVariable String id) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.close(id, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket kapatıldı");
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<TicketDto> cancel(@PathVariable String id, @RequestParam String reason) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.cancel(id, reason, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket iptal edildi");
    }

    @PatchMapping("/{id}/reopen")
    public ApiResponse<TicketDto> reopen(@PathVariable String id) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.reopen(id, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket yeniden açıldı");
    }

    @PatchMapping("/{id}/hold")
    public ApiResponse<TicketDto> putOnHold(@PathVariable String id, @RequestParam String reason) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.putOnHold(id, reason, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket beklemeye alındı");
    }

    // === ATAMA ===

    @PatchMapping("/{id}/assign")
    public ApiResponse<TicketDto> assign(
            @PathVariable String id,
            @RequestParam String agentId,
            @RequestParam(required = false) String agentName) {
        String currentAgentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.assignToAgent(id, agentId,
                agentName != null ? agentName : agentId, currentAgentId, currentAgentId);
        return ApiResponse.success(TicketDto.fromEntity(updated), "Ticket atandı");
    }

    // === CHECKLIST ===

    @PostMapping("/{id}/checklist")
    public ApiResponse<TicketDto> addChecklistItem(@PathVariable String id, @RequestParam String text) {
        Ticket updated = ticketService.addChecklistItem(id, text);
        return ApiResponse.success(TicketDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/checklist/{index}/toggle")
    public ApiResponse<TicketDto> toggleChecklistItem(@PathVariable String id, @PathVariable int index) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.toggleChecklistItem(id, index, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated));
    }

    @DeleteMapping("/{id}/checklist/{index}")
    public ApiResponse<TicketDto> removeChecklistItem(@PathVariable String id, @PathVariable int index) {
        Ticket updated = ticketService.removeChecklistItem(id, index);
        return ApiResponse.success(TicketDto.fromEntity(updated));
    }

    // === ETİKET & DUE DATE ===

    @PatchMapping("/{id}/tags/add")
    public ApiResponse<TicketDto> addTag(@PathVariable String id, @RequestParam String tag) {
        return ApiResponse.success(TicketDto.fromEntity(ticketService.addTag(id, tag)));
    }

    @PatchMapping("/{id}/tags/remove")
    public ApiResponse<TicketDto> removeTag(@PathVariable String id, @RequestParam String tag) {
        return ApiResponse.success(TicketDto.fromEntity(ticketService.removeTag(id, tag)));
    }

    @PatchMapping("/{id}/due-date")
    public ApiResponse<TicketDto> setDueDate(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate) {
        return ApiResponse.success(TicketDto.fromEntity(ticketService.setDueDate(id, dueDate)), "Bitiş tarihi güncellendi");
    }

    // === YORUM ===

    @PostMapping("/{id}/comment")
    public ApiResponse<TicketDto> addComment(@PathVariable String id, @RequestParam String comment) {
        String agentId = SecurityUtils.getCurrentUserId();
        Ticket updated = ticketService.addComment(id, comment, agentId, agentId);
        return ApiResponse.success(TicketDto.fromEntity(updated));
    }

    // === SLA & OVERDUE ===

    @GetMapping("/overdue")
    public ApiResponse<List<TicketDto>> getOverdue() {
        return ApiResponse.success(ticketService.findOverdueTickets()
                .stream().map(TicketDto::fromEntityBasic).toList());
    }

    @GetMapping("/sla-breached")
    public ApiResponse<List<TicketDto>> getSlaBreached() {
        return ApiResponse.success(ticketService.findSlaBreachedTickets()
                .stream().map(TicketDto::fromEntityBasic).toList());
    }

    @GetMapping("/due-soon")
    public ApiResponse<List<TicketDto>> getDueSoon(@RequestParam(defaultValue = "24") int hours) {
        return ApiResponse.success(ticketService.findTicketsDueSoon(hours)
                .stream().map(TicketDto::fromEntityBasic).toList());
    }

    // === İSTATİSTİK ===

    @GetMapping("/statistics")
    public ApiResponse<TicketService.TicketStatistics> getStatistics() {
        return ApiResponse.success(ticketService.getStatistics());
    }
}
