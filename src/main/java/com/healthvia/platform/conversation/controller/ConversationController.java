// conversation/controller/ConversationController.java
package com.healthvia.platform.conversation.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.auth.security.UserPrincipal;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.conversation.dto.ConversationDto;
import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.*;
import com.healthvia.platform.conversation.service.ConversationService;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.repository.LeadRepository;
import com.healthvia.platform.message.dto.MessageDto;
import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.entity.Message.MessageType;
import com.healthvia.platform.message.service.MessageService;

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
    private final MessageService messageService;
    private final LeadRepository leadRepository;

    // === CRUD ===

    @PostMapping
    public ApiResponse<ConversationDto> create(@Valid @RequestBody Conversation request) {
        Conversation created = conversationService.create(request);
        return ApiResponse.success(enrich(created), "Konuşma oluşturuldu");
    }

    @PostMapping("/start")
    public ApiResponse<ConversationDto> startConversation(
            @RequestParam String leadId,
            @RequestParam Channel channel) {
        String agentId = SecurityUtils.getCurrentUserId();
        Conversation conv = conversationService.getOrCreateForLead(leadId, channel, agentId);
        return ApiResponse.success(enrich(conv));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationDto> getById(@PathVariable String id) {
        return conversationService.findById(id)
                .map(this::enrich)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Konuşma bulunamadı"));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConversationDto> update(@PathVariable String id, @RequestBody Conversation request) {
        Conversation updated = conversationService.update(id, request);
        return ApiResponse.success(enrich(updated), "Konuşma güncellendi");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        conversationService.delete(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Konuşma silindi");
    }

    // === MESAJLAR (nested under conversation — frontend kontratı) ===

    /**
     * Konuşmaya ait tüm mesajlar, kronolojik.
     *
     * Frontend bu endpoint'i `/api/v1/conversations/{id}/messages` altında
     * bekliyor (`MessageController` ise mesajları `/api/v1/messages/...`
     * altında servis ediyor). Burada kısa bir proxy yazıp MessageService'e
     * delege ediyoruz — böylece path uyuşmazlığı inbox'ı boş bırakmıyor.
     */
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<MessageDto>> getMessages(@PathVariable String conversationId) {
        Pageable pageable = PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> page = messageService.findByConversation(conversationId, pageable);
        List<MessageDto> dtos = page.getContent().stream()
                .map(MessageDto::fromEntity)
                .toList();
        return ApiResponse.success(dtos);
    }

    /**
     * Agent yanıtı / dahili not gönderme. Frontend `{content, type, templateId}`
     * şeklinde body yolluyor. `type` "NOTE" ise dahili not, diğer durumlarda
     * standart agent mesajı olarak persist edip MessageService'e delege ediyoruz.
     */
    @PostMapping("/{conversationId}/messages")
    public ApiResponse<MessageDto> postMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessagePayload body) {
        String agentId = SecurityUtils.getCurrentUserId();
        String senderName = SecurityUtils.getCurrentUser()
                .map(UserPrincipal::getFullName)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(agentId);

        String type = body.getType() == null ? "TEXT" : body.getType().toUpperCase();
        String content = body.getContent() == null ? "" : body.getContent();

        Message persisted = switch (type) {
            case "NOTE" -> messageService.addInternalNote(conversationId, agentId, senderName, content);
            case "TEMPLATE" -> messageService.sendTemplateMessage(
                    conversationId, agentId, senderName, body.getTemplateId(), null, content);
            default -> messageService.sendAgentMessage(
                    conversationId, agentId, senderName, content, MessageType.TEXT, null);
        };
        return ApiResponse.success(MessageDto.fromEntity(persisted));
    }

    @lombok.Data
    public static class SendMessagePayload {
        private String content;
        private String type;
        private String templateId;
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
        return ApiResponse.success(enrichBasic(page));
    }

    @GetMapping("/my")
    public ApiResponse<Page<ConversationDto>> getMyConversations(
            @PageableDefault(size = 30) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        Page<Conversation> page = conversationService.findByAgent(agentId, pageable);
        return ApiResponse.success(enrichBasic(page));
    }

    @GetMapping("/my/status/{status}")
    public ApiResponse<Page<ConversationDto>> getMyByStatus(
            @PathVariable ConversationStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        Page<Conversation> page = conversationService.findByAgentAndStatus(agentId, status, pageable);
        return ApiResponse.success(enrichBasic(page));
    }

    @GetMapping("/my/unread")
    public ApiResponse<List<ConversationDto>> getMyUnread() {
        String agentId = SecurityUtils.getCurrentUserId();
        List<Conversation> convs = conversationService.findUnreadByAgent(agentId);
        return ApiResponse.success(enrichBasic(convs));
    }

    @GetMapping("/my/pinned")
    public ApiResponse<List<ConversationDto>> getMyPinned() {
        String agentId = SecurityUtils.getCurrentUserId();
        List<Conversation> convs = conversationService.findPinnedByAgent(agentId);
        return ApiResponse.success(enrichBasic(convs));
    }

    @GetMapping("/lead/{leadId}")
    public ApiResponse<List<ConversationDto>> getByLead(@PathVariable String leadId) {
        List<Conversation> convs = conversationService.findByLeadId(leadId);
        return ApiResponse.success(enrichBasic(convs));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<ConversationDto>> getByStatus(
            @PathVariable ConversationStatus status,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(enrichBasic(conversationService.findByStatus(status, pageable)));
    }

    @GetMapping("/search")
    public ApiResponse<Page<ConversationDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.success(enrichBasic(conversationService.search(keyword, pageable)));
    }

    // === DURUM İŞLEMLERİ ===

    @PatchMapping("/{id}/status")
    public ApiResponse<ConversationDto> changeStatus(
            @PathVariable String id,
            @RequestParam ConversationStatus status) {
        Conversation updated = conversationService.changeStatus(id, status);
        return ApiResponse.success(enrich(updated), "Durum güncellendi");
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<ConversationDto> resolve(@PathVariable String id) {
        Conversation updated = conversationService.resolve(id);
        return ApiResponse.success(enrich(updated), "Konuşma çözüldü");
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<ConversationDto> archive(@PathVariable String id) {
        Conversation updated = conversationService.archive(id);
        return ApiResponse.success(enrich(updated), "Konuşma arşivlendi");
    }

    @PatchMapping("/{id}/reopen")
    public ApiResponse<ConversationDto> reopen(@PathVariable String id) {
        Conversation updated = conversationService.reopen(id);
        return ApiResponse.success(enrich(updated), "Konuşma yeniden açıldı");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<ConversationDto> markAsRead(@PathVariable String id) {
        Conversation updated = conversationService.markAsRead(id);
        return ApiResponse.success(enrich(updated));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<ConversationDto> markAsReadPost(@PathVariable String id) {
        Conversation updated = conversationService.markAsRead(id);
        return ApiResponse.success(enrich(updated));
    }

    // === AGENT & ETİKET & PIN ===

    @PatchMapping("/{id}/assign")
    public ApiResponse<ConversationDto> assign(
            @PathVariable String id,
            @RequestParam String agentId) {
        Conversation updated = conversationService.assignToAgent(id, agentId);
        return ApiResponse.success(enrich(updated), "Konuşma atandı");
    }

    @PatchMapping("/{id}/tags/add")
    public ApiResponse<ConversationDto> addTag(@PathVariable String id, @RequestParam String tag) {
        Conversation updated = conversationService.addTag(id, tag);
        return ApiResponse.success(enrich(updated));
    }

    @PatchMapping("/{id}/tags/remove")
    public ApiResponse<ConversationDto> removeTag(@PathVariable String id, @RequestParam String tag) {
        Conversation updated = conversationService.removeTag(id, tag);
        return ApiResponse.success(enrich(updated));
    }

    @PatchMapping("/{id}/toggle-pin")
    public ApiResponse<ConversationDto> togglePin(@PathVariable String id) {
        Conversation updated = conversationService.togglePin(id);
        String msg = Boolean.TRUE.equals(updated.getIsPinned()) ? "Sabitlendi" : "Sabitleme kaldırıldı";
        return ApiResponse.success(enrich(updated), msg);
    }

    // === İLİŞKİ ===

    @PatchMapping("/{id}/link-ticket")
    public ApiResponse<ConversationDto> linkTicket(
            @PathVariable String id, @RequestParam String ticketId) {
        Conversation updated = conversationService.linkTicket(id, ticketId);
        return ApiResponse.success(enrich(updated));
    }

    @PatchMapping("/{id}/link-appointment")
    public ApiResponse<ConversationDto> linkAppointment(
            @PathVariable String id, @RequestParam String appointmentId) {
        Conversation updated = conversationService.linkAppointment(id, appointmentId);
        return ApiResponse.success(enrich(updated));
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

    // === HELPERS ===

    /** Single-conversation enrichment: one extra Lead query for the join. */
    private ConversationDto enrich(Conversation c) {
        if (c == null) return null;
        Lead lead = c.getLeadId() == null
                ? null
                : leadRepository.findById(c.getLeadId()).orElse(null);
        return ConversationDto.fromEntity(c, lead);
    }

    /** Page enrichment with a single batched Lead fetch (no N+1). */
    private Page<ConversationDto> enrichBasic(Page<Conversation> page) {
        Map<String, Lead> leadMap = batchFetchLeads(page.getContent());
        return page.map(c -> ConversationDto.fromEntityBasic(
                c, c.getLeadId() == null ? null : leadMap.get(c.getLeadId())));
    }

    /** List enrichment with a single batched Lead fetch. */
    private List<ConversationDto> enrichBasic(List<Conversation> convs) {
        Map<String, Lead> leadMap = batchFetchLeads(convs);
        return convs.stream()
                .map(c -> ConversationDto.fromEntityBasic(
                        c, c.getLeadId() == null ? null : leadMap.get(c.getLeadId())))
                .toList();
    }

    private Map<String, Lead> batchFetchLeads(Collection<Conversation> convs) {
        Set<String> ids = convs.stream()
                .map(Conversation::getLeadId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return StreamSupport.stream(leadRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(Lead::getId, l -> l));
    }
}
