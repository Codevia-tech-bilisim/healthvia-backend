// treatment/service/impl/TreatmentTypeServiceImpl.java
package com.healthvia.platform.treatment.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.treatment.entity.TreatmentType;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentCategory;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentStatus;
import com.healthvia.platform.treatment.repository.TreatmentTypeRepository;
import com.healthvia.platform.treatment.service.TreatmentTypeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TreatmentTypeServiceImpl implements TreatmentTypeService {

    private final TreatmentTypeRepository repository;

    // === CRUD ===

    @Override
    public TreatmentType create(TreatmentType treatmentType) {
        log.info("Creating treatment type: {}", treatmentType.getName());

        if (repository.existsBySlug(treatmentType.getSlug())) {
            throw new IllegalArgumentException("Bu slug zaten kullanılıyor: " + treatmentType.getSlug());
        }

        if (treatmentType.getStatus() == null) {
            treatmentType.setStatus(TreatmentStatus.ACTIVE);
        }
        if (treatmentType.getCurrency() == null) {
            treatmentType.setCurrency("USD");
        }

        return repository.save(treatmentType);
    }

    @Override
    public TreatmentType update(String id, TreatmentType updated) {
        TreatmentType existing = findByIdOrThrow(id);

        // Slug değişiyorsa benzersizlik kontrolü
        if (updated.getSlug() != null && !updated.getSlug().equals(existing.getSlug())) {
            if (repository.existsBySlug(updated.getSlug())) {
                throw new IllegalArgumentException("Bu slug zaten kullanılıyor: " + updated.getSlug());
            }
            existing.setSlug(updated.getSlug());
        }

        // Temel alanlar
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getNameTr() != null) existing.setNameTr(updated.getNameTr());
        if (updated.getSummary() != null) existing.setSummary(updated.getSummary());
        if (updated.getSummaryTr() != null) existing.setSummaryTr(updated.getSummaryTr());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getDescriptionTr() != null) existing.setDescriptionTr(updated.getDescriptionTr());

        // Kategori & sınıflandırma
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getSubCategory() != null) existing.setSubCategory(updated.getSubCategory());
        if (updated.getMedicalSpecialty() != null) existing.setMedicalSpecialty(updated.getMedicalSpecialty());

        // Görsel
        if (updated.getIconUrl() != null) existing.setIconUrl(updated.getIconUrl());
        if (updated.getCoverImageUrl() != null) existing.setCoverImageUrl(updated.getCoverImageUrl());
        if (updated.getGalleryUrls() != null) existing.setGalleryUrls(updated.getGalleryUrls());

        // Fiyatlandırma
        if (updated.getBasePrice() != null) existing.setBasePrice(updated.getBasePrice());
        if (updated.getMaxPrice() != null) existing.setMaxPrice(updated.getMaxPrice());
        if (updated.getCurrency() != null) existing.setCurrency(updated.getCurrency());
        if (updated.getPriceNote() != null) existing.setPriceNote(updated.getPriceNote());
        if (updated.getPriceNoteTr() != null) existing.setPriceNoteTr(updated.getPriceNoteTr());

        // Süre & lojistik
        if (updated.getProcedureDurationMinutes() != null) existing.setProcedureDurationMinutes(updated.getProcedureDurationMinutes());
        if (updated.getHospitalStayDays() != null) existing.setHospitalStayDays(updated.getHospitalStayDays());
        if (updated.getRecoveryDays() != null) existing.setRecoveryDays(updated.getRecoveryDays());
        if (updated.getTotalStayDays() != null) existing.setTotalStayDays(updated.getTotalStayDays());
        if (updated.getFollowUpRequired() != null) existing.setFollowUpRequired(updated.getFollowUpRequired());
        if (updated.getFollowUpNote() != null) existing.setFollowUpNote(updated.getFollowUpNote());

        // İlişkiler
        if (updated.getAvailableHospitalIds() != null) existing.setAvailableHospitalIds(updated.getAvailableHospitalIds());
        if (updated.getSpecialistDoctorIds() != null) existing.setSpecialistDoctorIds(updated.getSpecialistDoctorIds());
        if (updated.getPartnerHotelIds() != null) existing.setPartnerHotelIds(updated.getPartnerHotelIds());

        // FAQ & paket detayları
        if (updated.getFaqs() != null) existing.setFaqs(updated.getFaqs());
        if (updated.getInclusions() != null) existing.setInclusions(updated.getInclusions());
        if (updated.getInclusionsTr() != null) existing.setInclusionsTr(updated.getInclusionsTr());
        if (updated.getExclusions() != null) existing.setExclusions(updated.getExclusions());
        if (updated.getExclusionsTr() != null) existing.setExclusionsTr(updated.getExclusionsTr());

        // SEO
        if (updated.getSeoTitle() != null) existing.setSeoTitle(updated.getSeoTitle());
        if (updated.getSeoTitleTr() != null) existing.setSeoTitleTr(updated.getSeoTitleTr());
        if (updated.getSeoDescription() != null) existing.setSeoDescription(updated.getSeoDescription());
        if (updated.getSeoDescriptionTr() != null) existing.setSeoDescriptionTr(updated.getSeoDescriptionTr());
        if (updated.getSeoKeywords() != null) existing.setSeoKeywords(updated.getSeoKeywords());

        // Durum & sıralama
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getSortOrder() != null) existing.setSortOrder(updated.getSortOrder());

        log.info("Updated treatment type: {}", id);
        return repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TreatmentType> findById(String id) {
        return repository.findById(id).filter(t -> !t.isDeleted());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TreatmentType> findBySlug(String slug) {
        return repository.findBySlugAndDeletedFalse(slug);
    }

    @Override
    public void delete(String id, String deletedBy) {
        TreatmentType treatment = findByIdOrThrow(id);
        treatment.markAsDeleted(deletedBy);
        repository.save(treatment);
        log.info("Soft deleted treatment type: {} by: {}", id, deletedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TreatmentType> findAll(Pageable pageable) {
        return repository.findByStatusAndDeletedFalse(TreatmentStatus.ACTIVE, pageable);
    }

    // === SORGULAR ===

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findByCategory(TreatmentCategory category) {
        return repository.findByCategoryAndDeletedFalse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findActiveByCategory(TreatmentCategory category) {
        return repository.findByCategoryAndStatusAndDeletedFalse(category, TreatmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findAllActive() {
        return repository.findByStatusAndDeletedFalseOrderBySortOrderAsc(TreatmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findPopular() {
        return repository.findByIsPopularTrueAndStatusAndDeletedFalseOrderBySortOrderAsc(TreatmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findFeatured() {
        return repository.findByIsFeaturedTrueAndStatusAndDeletedFalseOrderBySortOrderAsc(TreatmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TreatmentType> search(String keyword, Pageable pageable) {
        return repository.searchByKeyword(keyword, pageable);
    }

    // === İLİŞKİ ===

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findByHospitalId(String hospitalId) {
        return repository.findByHospitalId(hospitalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findByDoctorId(String doctorId) {
        return repository.findByDoctorId(doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentType> findByPriceRange(BigDecimal min, BigDecimal max) {
        return repository.findByPriceRange(min, max);
    }

    // === YÖNETİM ===

    @Override
    public TreatmentType updateStatus(String id, TreatmentStatus status) {
        TreatmentType treatment = findByIdOrThrow(id);
        treatment.setStatus(status);
        log.info("Treatment {} status changed to {}", id, status);
        return repository.save(treatment);
    }

    @Override
    public TreatmentType togglePopular(String id) {
        TreatmentType treatment = findByIdOrThrow(id);
        treatment.setIsPopular(!treatment.getIsPopular());
        return repository.save(treatment);
    }

    @Override
    public TreatmentType toggleFeatured(String id) {
        TreatmentType treatment = findByIdOrThrow(id);
        treatment.setIsFeatured(!treatment.getIsFeatured());
        return repository.save(treatment);
    }

    @Override
    public TreatmentType addFaq(String id, TreatmentType.FAQ faq) {
        TreatmentType treatment = findByIdOrThrow(id);
        List<TreatmentType.FAQ> faqs = treatment.getFaqs();
        if (faqs == null) {
            faqs = new ArrayList<>();
        }
        faqs.add(faq);
        treatment.setFaqs(faqs);
        return repository.save(treatment);
    }

    @Override
    public TreatmentType removeFaq(String id, int faqIndex) {
        TreatmentType treatment = findByIdOrThrow(id);
        List<TreatmentType.FAQ> faqs = treatment.getFaqs();
        if (faqs != null && faqIndex >= 0 && faqIndex < faqs.size()) {
            faqs.remove(faqIndex);
            treatment.setFaqs(faqs);
        }
        return repository.save(treatment);
    }

    @Override
    public TreatmentType incrementViewCount(String id) {
        TreatmentType treatment = findByIdOrThrow(id);
        treatment.setViewCount(treatment.getViewCount() + 1);
        return repository.save(treatment);
    }

    @Override
    public TreatmentType incrementInquiryCount(String id) {
        TreatmentType treatment = findByIdOrThrow(id);
        treatment.setInquiryCount(treatment.getInquiryCount() + 1);
        return repository.save(treatment);
    }

    // === İSTATİSTİK ===

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return repository.countByDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCategory(TreatmentCategory category) {
        return repository.countByCategoryAndDeletedFalse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return repository.countByStatusAndDeletedFalse(TreatmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPopular() {
        return repository.countByIsPopularTrueAndDeletedFalse();
    }

    // === PRIVATE HELPER ===

    private TreatmentType findByIdOrThrow(String id) {
        return repository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("TreatmentType", "id", id));
    }
}
