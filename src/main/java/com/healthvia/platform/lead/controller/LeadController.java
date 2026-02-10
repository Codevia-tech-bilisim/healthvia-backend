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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Slf4j
public class LeadController {

    private final LeadService leadService;

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
    // ADMIN — CRUD
    // ===================================================================

    /**
     * Manuel lead oluştur (telefon, walk-in vb.)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> create(@Valid @RequestBody Lead request) {
        log.info("Creating lead manually: {}", request.getSource());
        Lead created = leadService.create(request);
        return ApiResponse.success(LeadDto.fromEntity(created), "Lead oluşturuldu");
    }

    /**
     * Lead güncelle
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> update(@PathVariable String id, @RequestBody Lead request) {
        Lead updated = leadService.update(id, request);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead güncellendi");
    }

    /**
     * Lead detayı
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> getById(@PathVariable String id) {
        return leadService.findById(id)
                .map(LeadDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Lead bulunamadı"));
    }

    /**
     * Lead sil
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        String deletedBy = SecurityUtils.getCurrentUserId();
        leadService.delete(id, deletedBy);
        return ApiResponse.success("Lead silindi");
    }

    // ===================================================================
    // ADMIN — LİSTELEME & FİLTRELEME
    // ===================================================================

    /**
     * Tüm leadler
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<LeadDto>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        Page<Lead> leads = leadService.findAll(pageable);
        return ApiResponse.success(leads.map(LeadDto::fromEntityBasic));
    }

    /**
     * Duruma göre
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<LeadDto>> getByStatus(
            @PathVariable LeadStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Lead> leads = leadService.findByStatus(status, pageable);
        return ApiResponse.success(leads.map(LeadDto::fromEntityBasic));
    }

    /**
     * Bana atanmış leadler
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<LeadDto>> getMyLeads(@PageableDefault(size = 20) Pageable pageable) {
        String agentId = SecurityUtils.getCurrentUserId();
        Page<Lead> leads = leadService.findByAgent(agentId, pageable);
        return ApiResponse.success(leads.map(LeadDto::fromEntityBasic));
    }

    /**
     * Kaynağa göre
     */
    @GetMapping("/source/{source}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<LeadDto>> getBySource(
            @PathVariable LeadSource source,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(leadService.findBySource(source, pageable).map(LeadDto::fromEntityBasic));
    }

    /**
     * Etiketlere göre
     */
    @GetMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<LeadDto>> getByTags(
            @RequestParam List<String> tags,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(leadService.findByTags(tags, pageable).map(LeadDto::fromEntityBasic));
    }

    /**
     * Arama
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<LeadDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(leadService.search(keyword, pageable).map(LeadDto::fromEntityBasic));
    }

    /**
     * Atanmamış leadler
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LeadDto>> getUnassigned() {
        List<Lead> leads = leadService.findUnassigned();
        return ApiResponse.success(leads.stream().map(LeadDto::fromEntityBasic).toList());
    }

    // ===================================================================
    // ADMIN — DURUM & ATAMA İŞLEMLERİ
    // ===================================================================

    /**
     * Durum değiştir
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> changeStatus(
            @PathVariable String id,
            @RequestParam LeadStatus status,
            @RequestParam(required = false) String reason) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.changeStatus(id, status, changedBy, reason);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Durum güncellendi");
    }

    /**
     * Agent'a ata
     */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> assign(
            @PathVariable String id,
            @RequestParam String agentId) {
        Lead updated = leadService.assignToAgent(id, agentId, AssignmentMethod.MANUAL);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead atandı");
    }

    /**
     * Otomatik ata
     */
    @PatchMapping("/{id}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> autoAssign(@PathVariable String id) {
        Lead updated = leadService.autoAssign(id);
        if (updated.getAssignedAgentId() != null) {
            return ApiResponse.success(LeadDto.fromEntity(updated), "Lead otomatik atandı");
        }
        return ApiResponse.error("Müsait agent bulunamadı");
    }

    /**
     * Başka agent'a transfer et
     */
    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> transfer(
            @PathVariable String id,
            @RequestParam String newAgentId) {
        String transferredBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.transferToAgent(id, newAgentId, transferredBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead transfer edildi");
    }

    /**
     * Dönüştür (hasta kaydı oluştur)
     */
    @PatchMapping("/{id}/convert")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> convert(
            @PathVariable String id,
            @RequestParam String patientId,
            @RequestParam(required = false) BigDecimal conversionValue) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.markAsConverted(id, patientId, conversionValue, changedBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead dönüştürüldü");
    }

    /**
     * Kaybedildi olarak işaretle
     */
    @PatchMapping("/{id}/lost")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> markAsLost(
            @PathVariable String id,
            @RequestParam String reason) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.markAsLost(id, reason, changedBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead kaybedildi olarak işaretlendi");
    }

    /**
     * Spam olarak işaretle
     */
    @PatchMapping("/{id}/spam")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> markAsSpam(@PathVariable String id) {
        String changedBy = SecurityUtils.getCurrentUserId();
        Lead updated = leadService.markAsSpam(id, changedBy);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Lead spam olarak işaretlendi");
    }

    // ===================================================================
    // ADMIN — TAKİP & ETİKET
    // ===================================================================

    /**
     * Takip planla
     */
    @PatchMapping("/{id}/follow-up")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> scheduleFollowUp(
            @PathVariable String id,
            @RequestParam LocalDateTime followUpAt) {
        Lead updated = leadService.scheduleFollowUp(id, followUpAt);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Takip planlandı");
    }

    /**
     * Takip gereken leadler
     */
    @GetMapping("/needs-follow-up")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LeadDto>> getNeedingFollowUp() {
        List<Lead> leads = leadService.findLeadsNeedingFollowUp();
        return ApiResponse.success(leads.stream().map(LeadDto::fromEntityBasic).toList());
    }

    /**
     * Etiket ekle
     */
    @PatchMapping("/{id}/tags/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> addTag(@PathVariable String id, @RequestParam String tag) {
        Lead updated = leadService.addTag(id, tag);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Etiket eklendi");
    }

    /**
     * Etiket kaldır
     */
    @PatchMapping("/{id}/tags/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> removeTag(@PathVariable String id, @RequestParam String tag) {
        Lead updated = leadService.removeTag(id, tag);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Etiket kaldırıldı");
    }

    /**
     * Hasta ile eşleştir
     */
    @PatchMapping("/{id}/link-patient")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeadDto> linkToPatient(
            @PathVariable String id,
            @RequestParam String patientId) {
        Lead updated = leadService.linkToPatient(id, patientId);
        return ApiResponse.success(LeadDto.fromEntity(updated), "Hasta ile eşleştirildi");
    }

    // ===================================================================
    // ADMIN — İSTATİSTİK
    // ===================================================================

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
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
