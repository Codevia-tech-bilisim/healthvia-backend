// conversation/controller/ConversationController.java
package com.healthvia.platform.conversation.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.conversation.dto.ConversationDto;
import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.*;
import com.healthvia.platform.conversation.service.ConversationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/conversations")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','CEO','AGENT')")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;

    // === CRUD ===

    @PostMapping
    public ApiResponse<ConversationDto> create(@Valid @RequestBody Conversation request) {
        Conversation created = conversationService.create(request);
        return ApiResponse.success(ConversationDto.fromEntity(created), "Konuşma oluşturuldu");
    }

    @PostMapping("/start")
    public ApiResponse<ConversationDto> startConversation(
            @RequestParam String leadId,
            @RequestParam Channel channel) {
        String agentId = SecurityUtils.getCurrentUserId();
        Conversation conv = conversationService.getOrCreateForLead(leadId, channel, agentId);
        return ApiResponse.success(ConversationDto.fromEntity(conv));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationDto> getById(@PathVariable String id) {
        return conversationService.findById(id)
                .map(ConversationDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Konuşma bulunamadı"));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConversationDto> update(@PathVariable String id, @RequestBody Conversation request) {
        Conversation updated = conversationService.update(id, request);
        return ApiResponse.success(ConversationDto.fromEntity(updated), "Konuşma güncellendi");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        conversationService.delete(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Konuşma silindi");
    }

    // === LİSTELEME ===

    /**
     * Generic inbox listing for the agent dashboard. Combines optional
     * assignedAgentId / channel / free-text filters into one query. When all
     * filters are absent, returns every non-deleted conversation ordered by
     * lastMessageAt desc.
     */
    @GetMapping
    public ApiResponse<Page<ConversationDto>> list(
            @RequestParam(required = false) String assignedAgentId,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 30) Pageable pageable) {
        Page<Conversation> page = conversationService.list(assignedAgentId, channel, search, pageable);
        return ApiResponse.success(page.map(ConversationDto::fromEntityBasic));
    }

    @GetMapping("/my")
    public ApiResponse<Page<ConversationDto>> getMyConversations(
            @PageableDefault(size = 30) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        Page<Conversation> page = conversationService.findByAgent(agentId, pageable);
        return ApiResponse.success(page.map(ConversationDto::fromEntityBasic));
    }

    @GetMapping("/my/status/{status}")
    public ApiResponse<Page<ConversationDto>> getMyByStatus(
            @PathVariable ConversationStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        Page<Conversation> page = conversationService.findByAgentAndStatus(agentId, status, pageable);
        return ApiResponse.success(page.map(ConversationDto::fromEntityBasic));
    }

    @GetMapping("/my/unread")
    public ApiResponse<List<ConversationDto>> getMyUnread() {
        String agentId = SecurityUtils.getCurrentUserId();
        List<Conversation> convs = conversationService.findUnreadByAgent(agentId);
        return ApiResponse.success(convs.stream().map(ConversationDto::fromEntityBasic).toList());
    }

    @GetMapping("/my/pinned")
    public ApiResponse<List<ConversationDto>> getMyPinned() {
        String agentId = SecurityUtils.getCurrentUserId();
        List<Conversation> convs = conversationService.findPinnedByAgent(agentId);
        return ApiResponse.success(convs.stream().map(ConversationDto::fromEntityBasic).toList());
    }

    @GetMapping("/lead/{leadId}")
    public ApiResponse<List<ConversationDto>> getByLead(@PathVariable String leadId) {
        List<Conversation> convs = conversationService.findByLeadId(leadId);
        return ApiResponse.success(convs.stream().map(ConversationDto::fromEntityBasic).toList());
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<ConversationDto>> getByStatus(
            @PathVariable ConversationStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(conversationService.findByStatus(status, pageable)
                .map(ConversationDto::fromEntityBasic));
    }

    @GetMapping("/search")
    public ApiResponse<Page<ConversationDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(conversationService.search(keyword, pageable)
                .map(ConversationDto::fromEntityBasic));
    }

    // === DURUM İŞLEMLERİ ===

    @PatchMapping("/{id}/status")
    public ApiResponse<ConversationDto> changeStatus(
            @PathVariable String id,
            @RequestParam ConversationStatus status) {
        Conversation updated = conversationService.changeStatus(id, status);
        return ApiResponse.success(ConversationDto.fromEntity(updated), "Durum güncellendi");
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<ConversationDto> resolve(@PathVariable String id) {
        Conversation updated = conversationService.resolve(id);
        return ApiResponse.success(ConversationDto.fromEntity(updated), "Konuşma çözüldü");
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<ConversationDto> archive(@PathVariable String id) {
        Conversation updated = conversationService.archive(id);
        return ApiResponse.success(ConversationDto.fromEntity(updated), "Konuşma arşivlendi");
    }

    @PatchMapping("/{id}/reopen")
    public ApiResponse<ConversationDto> reopen(@PathVariable String id) {
        Conversation updated = conversationService.reopen(id);
        return ApiResponse.success(ConversationDto.fromEntity(updated), "Konuşma yeniden açıldı");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<ConversationDto> markAsRead(@PathVariable String id) {
        Conversation updated = conversationService.markAsRead(id);
        return ApiResponse.success(ConversationDto.fromEntity(updated));
    }

    // === AGENT & ETİKET & PIN ===

    @PatchMapping("/{id}/assign")
    public ApiResponse<ConversationDto> assign(
            @PathVariable String id,
            @RequestParam String agentId) {
        Conversation updated = conversationService.assignToAgent(id, agentId);
        return ApiResponse.success(ConversationDto.fromEntity(updated), "Konuşma atandı");
    }

    @PatchMapping("/{id}/tags/add")
    public ApiResponse<ConversationDto> addTag(@PathVariable String id, @RequestParam String tag) {
        Conversation updated = conversationService.addTag(id, tag);
        return ApiResponse.success(ConversationDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/tags/remove")
    public ApiResponse<ConversationDto> removeTag(@PathVariable String id, @RequestParam String tag) {
        Conversation updated = conversationService.removeTag(id, tag);
        return ApiResponse.success(ConversationDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/toggle-pin")
    public ApiResponse<ConversationDto> togglePin(@PathVariable String id) {
        Conversation updated = conversationService.togglePin(id);
        String msg = Boolean.TRUE.equals(updated.getIsPinned()) ? "Sabitlendi" : "Sabitleme kaldırıldı";
        return ApiResponse.success(ConversationDto.fromEntity(updated), msg);
    }

    // === İLİŞKİ ===

    @PatchMapping("/{id}/link-ticket")
    public ApiResponse<ConversationDto> linkTicket(
            @PathVariable String id, @RequestParam String ticketId) {
        Conversation updated = conversationService.linkTicket(id, ticketId);
        return ApiResponse.success(ConversationDto.fromEntity(updated));
    }

    @PatchMapping("/{id}/link-appointment")
    public ApiResponse<ConversationDto> linkAppointment(
            @PathVariable String id, @RequestParam String appointmentId) {
        Conversation updated = conversationService.linkAppointment(id, appointmentId);
        return ApiResponse.success(ConversationDto.fromEntity(updated));
    }

    // === İSTATİSTİK ===

    @GetMapping("/statistics")
    public ApiResponse<ConversationStatistics> getStatistics() {
        String agentId = SecurityUtils.getCurrentUserId();
        ConversationStatistics stats = new ConversationStatistics();
        stats.setTotal(conversationService.countAll());
        stats.setOpen(conversationService.countByStatus(ConversationStatus.OPEN));
        stats.setWaitingReply(conversationService.countByStatus(ConversationStatus.WAITING_REPLY));
        stats.setAgentReply(conversationService.countByStatus(ConversationStatus.AGENT_REPLY));
        stats.setResolved(conversationService.countByStatus(ConversationStatus.RESOLVED));
        stats.setMyUnread(conversationService.countUnreadByAgent(agentId));
        return ApiResponse.success(stats);
    }

    @lombok.Data
    public static class ConversationStatistics {
        private long total;
        private long open;
        private long waitingReply;
        private long agentReply;
        private long resolved;
        private long myUnread;
    }
}
