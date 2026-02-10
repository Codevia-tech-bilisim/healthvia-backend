// treatment/controller/TreatmentTypeController.java
package com.healthvia.platform.treatment.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.treatment.dto.TreatmentTypeDto;
import com.healthvia.platform.treatment.entity.TreatmentType;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentCategory;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentStatus;
import com.healthvia.platform.treatment.service.TreatmentTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/treatments")
@RequiredArgsConstructor
@Slf4j
public class TreatmentTypeController {

    private final TreatmentTypeService treatmentTypeService;

    // ===================================================================
    // PUBLIC ENDPOINTS — Herkes erişebilir
    // ===================================================================

    /**
     * Aktif tedavileri listele (paginated)
     */
    @GetMapping("/public")
    public ApiResponse<Page<TreatmentTypeDto>> getActiveTreatments(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<TreatmentType> treatments = treatmentTypeService.findAll(pageable);
        Page<TreatmentTypeDto> dtos = treatments.map(TreatmentTypeDto::fromEntityBasic);
        return ApiResponse.success(dtos);
    }

    /**
     * Slug ile tedavi detayı (public)
     */
    @GetMapping("/public/{slug}")
    public ApiResponse<TreatmentTypeDto> getBySlug(@PathVariable String slug) {
        treatmentTypeService.incrementViewCount(
                treatmentTypeService.findBySlug(slug)
                        .orElseThrow(() -> new RuntimeException("Tedavi bulunamadı: " + slug))
                        .getId()
        );

        return treatmentTypeService.findBySlug(slug)
                .map(TreatmentTypeDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Tedavi bulunamadı: " + slug));
    }

    /**
     * Kategoriye göre listele
     */
    @GetMapping("/public/category/{category}")
    public ApiResponse<List<TreatmentTypeDto>> getByCategory(@PathVariable TreatmentCategory category) {
        List<TreatmentType> treatments = treatmentTypeService.findActiveByCategory(category);
        List<TreatmentTypeDto> dtos = treatments.stream()
                .map(TreatmentTypeDto::fromEntityBasic)
                .toList();
        return ApiResponse.success(dtos);
    }

    /**
     * Popüler tedaviler
     */
    @GetMapping("/public/popular")
    public ApiResponse<List<TreatmentTypeDto>> getPopular() {
        List<TreatmentType> treatments = treatmentTypeService.findPopular();
        List<TreatmentTypeDto> dtos = treatments.stream()
                .map(TreatmentTypeDto::fromEntityBasic)
                .toList();
        return ApiResponse.success(dtos);
    }

    /**
     * Öne çıkan tedaviler (homepage)
     */
    @GetMapping("/public/featured")
    public ApiResponse<List<TreatmentTypeDto>> getFeatured() {
        List<TreatmentType> treatments = treatmentTypeService.findFeatured();
        List<TreatmentTypeDto> dtos = treatments.stream()
                .map(TreatmentTypeDto::fromEntityBasic)
                .toList();
        return ApiResponse.success(dtos);
    }

    /**
     * Tedavi arama
     */
    @GetMapping("/public/search")
    public ApiResponse<Page<TreatmentTypeDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<TreatmentType> treatments = treatmentTypeService.search(keyword, pageable);
        Page<TreatmentTypeDto> dtos = treatments.map(TreatmentTypeDto::fromEntityBasic);
        return ApiResponse.success(dtos);
    }

    /**
     * Fiyat aralığına göre ara
     */
    @GetMapping("/public/price-range")
    public ApiResponse<List<TreatmentTypeDto>> getByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {

        List<TreatmentType> treatments = treatmentTypeService.findByPriceRange(min, max);
        List<TreatmentTypeDto> dtos = treatments.stream()
                .map(TreatmentTypeDto::fromEntityBasic)
                .toList();
        return ApiResponse.success(dtos);
    }

    /**
     * Doktora göre tedaviler
     */
    @GetMapping("/public/by-doctor/{doctorId}")
    public ApiResponse<List<TreatmentTypeDto>> getByDoctor(@PathVariable String doctorId) {
        List<TreatmentType> treatments = treatmentTypeService.findByDoctorId(doctorId);
        List<TreatmentTypeDto> dtos = treatments.stream()
                .map(TreatmentTypeDto::fromEntityBasic)
                .toList();
        return ApiResponse.success(dtos);
    }

    /**
     * Hastaneye göre tedaviler
     */
    @GetMapping("/public/by-hospital/{hospitalId}")
    public ApiResponse<List<TreatmentTypeDto>> getByHospital(@PathVariable String hospitalId) {
        List<TreatmentType> treatments = treatmentTypeService.findByHospitalId(hospitalId);
        List<TreatmentTypeDto> dtos = treatments.stream()
                .map(TreatmentTypeDto::fromEntityBasic)
                .toList();
        return ApiResponse.success(dtos);
    }

    // ===================================================================
    // ADMIN ENDPOINTS — Sadece ADMIN erişebilir
    // ===================================================================

    /**
     * Yeni tedavi oluştur
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> create(@Valid @RequestBody TreatmentType request) {
        log.info("Creating treatment type: {}", request.getName());
        TreatmentType created = treatmentTypeService.create(request);
        return ApiResponse.success(TreatmentTypeDto.fromEntity(created), "Tedavi başarıyla oluşturuldu");
    }

    /**
     * Tedavi güncelle
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> update(
            @PathVariable String id,
            @RequestBody TreatmentType request) {

        log.info("Updating treatment type: {}", id);
        TreatmentType updated = treatmentTypeService.update(id, request);
        return ApiResponse.success(TreatmentTypeDto.fromEntity(updated), "Tedavi güncellendi");
    }

    /**
     * ID ile detay (admin — tüm alanlar)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> getById(@PathVariable String id) {
        return treatmentTypeService.findById(id)
                .map(TreatmentTypeDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Tedavi bulunamadı"));
    }

    /**
     * Tedavi sil (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        String deletedBy = SecurityUtils.getCurrentUserId();
        treatmentTypeService.delete(id, deletedBy);
        return ApiResponse.success("Tedavi silindi");
    }

    /**
     * Durum değiştir
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> updateStatus(
            @PathVariable String id,
            @RequestParam TreatmentStatus status) {

        TreatmentType updated = treatmentTypeService.updateStatus(id, status);
        return ApiResponse.success(TreatmentTypeDto.fromEntity(updated), "Durum güncellendi");
    }

    /**
     * Popüler toggle
     */
    @PatchMapping("/{id}/toggle-popular")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> togglePopular(@PathVariable String id) {
        TreatmentType updated = treatmentTypeService.togglePopular(id);
        String msg = Boolean.TRUE.equals(updated.getIsPopular()) ? "Popüler olarak işaretlendi" : "Popüler kaldırıldı";
        return ApiResponse.success(TreatmentTypeDto.fromEntity(updated), msg);
    }

    /**
     * Öne çıkan toggle
     */
    @PatchMapping("/{id}/toggle-featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> toggleFeatured(@PathVariable String id) {
        TreatmentType updated = treatmentTypeService.toggleFeatured(id);
        String msg = Boolean.TRUE.equals(updated.getIsFeatured()) ? "Öne çıkarıldı" : "Öne çıkarmadan kaldırıldı";
        return ApiResponse.success(TreatmentTypeDto.fromEntity(updated), msg);
    }

    /**
     * FAQ ekle
     */
    @PostMapping("/{id}/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> addFaq(
            @PathVariable String id,
            @Valid @RequestBody TreatmentTypeDto.FaqDto faqDto) {

        TreatmentType updated = treatmentTypeService.addFaq(id, faqDto.toEntity());
        return ApiResponse.success(TreatmentTypeDto.fromEntity(updated), "FAQ eklendi");
    }

    /**
     * FAQ sil
     */
    @DeleteMapping("/{id}/faqs/{faqIndex}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentTypeDto> removeFaq(
            @PathVariable String id,
            @PathVariable int faqIndex) {

        TreatmentType updated = treatmentTypeService.removeFaq(id, faqIndex);
        return ApiResponse.success(TreatmentTypeDto.fromEntity(updated), "FAQ silindi");
    }

    // ===================================================================
    // İSTATİSTİK ENDPOINTS — Admin
    // ===================================================================

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TreatmentStatistics> getStatistics() {
        TreatmentStatistics stats = new TreatmentStatistics();
        stats.setTotal(treatmentTypeService.countAll());
        stats.setActive(treatmentTypeService.countActive());
        stats.setPopular(treatmentTypeService.countPopular());
        return ApiResponse.success(stats);
    }

    @GetMapping("/statistics/by-category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> countByCategory(@PathVariable TreatmentCategory category) {
        return ApiResponse.success(treatmentTypeService.countByCategory(category));
    }

    // === Inner Class ===

    @lombok.Data
    public static class TreatmentStatistics {
        private long total;
        private long active;
        private long popular;
    }
}
