// lead/controller/LeadController.java
package com.healthvia.platform.lead.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.lead.dto.LeadDto;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.*;
import com.healthvia.platform.lead.service.LeadService;
import com.healthvia.platform.patientcase.service.PatientCaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lead REST endpoints.
 *
 * Authorization: webhook/public endpoints stay open; every authenticated
 * endpoint allows SUPERADMIN, ADMIN, CEO and AGENT because the agent
 * dashboard inbox needs read+write access to leads it's working on.
 * Spring's hasRole('ADMIN') matches the exact role name only, so the
 * previous guard locked out SUPERADMIN and AGENT entirely — which is why
 * the inbox side panel surfaced "yetkiniz yok" the moment a conversation
 * was opened.
 */
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Slf4j
public class LeadController {

    private static final String STAFF_ROLES =
            "hasAnyRole('SUPERADMIN','ADMIN','CEO','AGENT')";

    private final LeadService leadService;
    private final PatientCaseService patientCaseService;

    // ===================================================================
    // PUBLIC / WEBHOOK — Lead oluşturma (form, WhatsApp webhook vb.)
    // ===================================================================

    /**
     * Web formundan lead oluştur
     */
    @PostMapping("/public/form")
    public ApiResponse<LeadDto> createFromForm(@Valid @RequestBody Lead request) {
        request.setSource(LeadSource.WEB_FORM);
        Lead created = leadService.create(request);

        // Otomatik atama dene
        leadService.autoAssign(created.getId());

        Lead refreshed = leadService.findById(created.getId()).orElse(created);
        return ApiResponse.success(LeadDto.fromEntity(refreshed), "Talebiniz alındı, en kısa sürede dönüş yapılacaktır");
    }

    /**
     * WhatsApp webhook'undan lead oluştur
     */
    @PostMapping("/webhook/whatsapp")
    public ApiResponse<LeadDto> createFromWhatsApp(@RequestBody Lead request) {
        request.setSource(LeadSource.WHATSAPP);
        Lead created = leadService.create(request);
        leadService.autoAssign(created.getId());

        Lead refreshed = leadService.findById(created.getId()).orElse(created);
        return ApiResponse.success(LeadDto.fromEntity(refreshed));
    }

    /**
     * Instagram webhook'undan lead oluştur
     */
    @PostMapping("/webhook/instagram")
    public ApiResponse<LeadDto> createFromInstagram(@RequestBody Lead request) {
        request.setSource(LeadSource.INSTAGRAM);
        Lead created = leadService.create(request);
        leadService.autoAssign(created.getId());

        Lead refreshed = leadService.findById(created.getId()).orElse(created);
        return ApiResponse.success(LeadDto.fromEntity(refreshed));
    }

    /**
     * Email'den lead oluştur
     */
    @PostMapping("/webhook/email")
    public ApiResponse<LeadDto> createFromEmail(@RequestBody Lead request) {
        request.setSource(LeadSource.EMAIL);
        Lead created = leadService.create(request);
        leadService.autoAssign(created.getId());

        Lead refreshed = leadService.findById(created.getId()).orElse(created);
        return ApiResponse.success(LeadDto.fromEntity(refreshed));
    }

    // ===================================================================
    // STAFF — CRUD
    // ===================================================================

    @PostMapping
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> create(@Valid @RequestBody Lead request) {
        log.info("Creating lead manually: {}", request.getSource());
        Lead created = leadService.create(request);
        return ApiResponse.success(LeadDto.fromEntity(created), "Lead oluşturuldu");
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> update(@PathVariable String id, @RequestBody Lead request) {
        Lead updated = leadService.update(id, request);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead güncellendi");
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> getById(@PathVariable String id) {
        return leadService.findById(id)
                .map(LeadDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Lead bulunamadı"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Void> delete(@PathVariable String id) {
        String deletedBy = SecurityUtils.getCurrentUserId();
        leadService.delete(id, deletedBy);
        return ApiResponse.success("Lead silindi");
    }

    // ===================================================================
    // STAFF — LİSTELEME & FİLTRELEME
    // ===================================================================

    @GetMapping
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Page<LeadDto>> getAll(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) String assignedAgentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean unassigned,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Lead> leads = leadService.findWithFilters(
                status, source, priority, assignedAgentId, keyword, unassigned, pageable);
        return ApiResponse.success(leads.map(LeadDto::fromEntityBasic));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Page<LeadDto>> getByStatus(
            @PathVariable LeadStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Lead> leads = leadService.findByStatus(status, pageable);
        return ApiResponse.success(leads.map(LeadDto::fromEntityBasic));
    }

    @GetMapping("/my")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Page<LeadDto>> getMyLeads(@PageableDefault(size = 20) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        Page<Lead> leads = leadService.findByAgent(agentId, pageable);
        return ApiResponse.success(leads.map(LeadDto::fromEntityBasic));
    }

    @GetMapping("/source/{source}")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Page<LeadDto>> getBySource(
            @PathVariable LeadSource source,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(leadService.findBySource(source, pageable).map(LeadDto::fromEntityBasic));
    }

    @GetMapping("/tags")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Page<LeadDto>> getByTags(
            @RequestParam List<String> tags,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(leadService.findByTags(tags, pageable).map(LeadDto::fromEntityBasic));
    }

    @GetMapping("/search")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<Page<LeadDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(leadService.search(keyword, pageable).map(LeadDto::fromEntityBasic));
    }

    @GetMapping("/unassigned")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<List<LeadDto>> getUnassigned() {
        List<Lead> leads = leadService.findUnassigned();
        return ApiResponse.success(leads.stream().map(LeadDto::fromEntityBasic).toList());
    }

    // ===================================================================
    // STAFF — DURUM & ATAMA İŞLEMLERİ
    // ===================================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> changeStatus(
            @PathVariable String id,
            @RequestParam LeadStatus status,
            @RequestParam(required = false) String reason) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.changeStatus(id, status, changedBy, reason);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Durum güncellendi");
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> assign(
            @PathVariable String id,
            @RequestParam String agentId) {
        Lead updated = leadService.assignToAgent(id, agentId, AssignmentMethod.MANUAL);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead atandı");
    }

    @PatchMapping("/{id}/auto-assign")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> autoAssign(@PathVariable String id) {
        Lead updated = leadService.autoAssign(id);
        if (updated.getAssignedAgentId() != null) {
            return ApiResponse.success(LeadDto.fromEntity(updated), "Lead otomatik atandı");
        }
        return ApiResponse.error("Müsait agent bulunamadı");
    }

    @PatchMapping("/{id}/transfer")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> transfer(
            @PathVariable String id,
            @RequestParam String newAgentId) {
        String transferredBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.transferToAgent(id, newAgentId, transferredBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead transfer edildi");
    }

    @PatchMapping("/{id}/convert")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> convert(
            @PathVariable String id,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) BigDecimal conversionValue) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.markAsConverted(id, patientId, conversionValue, changedBy);
        // Dönüşümde otomatik olarak bir PatientCase aç (idempotent — varsa onu döner).
        try {
            patientCaseService.createFromLead(id);
        } catch (Exception e) {
            log.warn("Lead {} dönüştürüldü ama case oluşturulamadı: {}", id, e.getMessage());
        }
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead dönüştürüldü");
    }

    @PatchMapping("/{id}/lost")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> markAsLost(
            @PathVariable String id,
            @RequestParam String reason) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.markAsLost(id, reason, changedBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead kaybedildi olarak işaretlendi");
    }

    @PatchMapping("/{id}/spam")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> markAsSpam(@PathVariable String id) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.markAsSpam(id, changedBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead spam olarak işaretlendi");
    }

    // ===================================================================
    // STAFF — TAKİP & ETİKET
    // ===================================================================

    @PatchMapping("/{id}/follow-up")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> scheduleFollowUp(
            @PathVariable String id,
            @RequestParam LocalDateTime followUpAt) {
        Lead updated = leadService.scheduleFollowUp(id, followUpAt);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Takip planlandı");
    }

    @GetMapping("/needs-follow-up")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<List<LeadDto>> getNeedingFollowUp() {
        List<Lead> leads = leadService.findLeadsNeedingFollowUp();
        return ApiResponse.success(leads.stream().map(LeadDto::fromEntityBasic).toList());
    }

    @PatchMapping("/{id}/tags/add")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> addTag(@PathVariable String id, @RequestParam String tag) {
        Lead updated = leadService.addTag(id, tag);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Etiket eklendi");
    }

    @PatchMapping("/{id}/tags/remove")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> removeTag(@PathVariable String id, @RequestParam String tag) {
        Lead updated = leadService.removeTag(id, tag);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Etiket kaldırıldı");
    }

    @PatchMapping("/{id}/link-patient")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadDto> linkToPatient(
            @PathVariable String id,
            @RequestParam String patientId) {
        Lead updated = leadService.linkToPatient(id, patientId);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Hasta ile eşleştirildi");
    }

    // ===================================================================
    // STAFF — İSTATİSTİK
    // ===================================================================

    @GetMapping("/statistics")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<LeadStatistics> getStatistics() {
        LeadStatistics stats = new LeadStatistics();
        stats.setTotal(leadService.countAll());
        stats.setNewCount(leadService.countByStatus(LeadStatus.NEW));
        stats.setAssignedCount(leadService.countByStatus(LeadStatus.ASSIGNED));
        stats.setQualifiedCount(leadService.countByStatus(LeadStatus.QUALIFIED));
        stats.setConvertedCount(leadService.countByStatus(LeadStatus.CONVERTED));
        stats.setLostCount(leadService.countByStatus(LeadStatus.LOST));
        stats.setNeedingFollowUp((long) leadService.findLeadsNeedingFollowUp().size());
        return ApiResponse.success(stats);
    }

    @lombok.Data
    public static class LeadStatistics {
        private long total;
        private long newCount;
        private long assignedCount;
        private long qualifiedCount;
        private long convertedCount;
        private long lostCount;
        private long needingFollowUp;
    }
}
