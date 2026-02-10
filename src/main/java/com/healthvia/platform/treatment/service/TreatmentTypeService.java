// treatment/service/TreatmentTypeService.java
package com.healthvia.platform.treatment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.treatment.entity.TreatmentType;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentCategory;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentStatus;

public interface TreatmentTypeService {

    // === CRUD ===
    TreatmentType create(TreatmentType treatmentType);
    TreatmentType update(String id, TreatmentType treatmentType);
    Optional<TreatmentType> findById(String id);
    Optional<TreatmentType> findBySlug(String slug);
    void delete(String id, String deletedBy);
    Page<TreatmentType> findAll(Pageable pageable);

    // === SORGULAR ===
    List<TreatmentType> findByCategory(TreatmentCategory category);
    List<TreatmentType> findActiveByCategory(TreatmentCategory category);
    List<TreatmentType> findAllActive();
    List<TreatmentType> findPopular();
    List<TreatmentType> findFeatured();
    Page<TreatmentType> search(String keyword, Pageable pageable);

    // === İLİŞKİ ===
    List<TreatmentType> findByHospitalId(String hospitalId);
    List<TreatmentType> findByDoctorId(String doctorId);
    List<TreatmentType> findByPriceRange(BigDecimal min, BigDecimal max);

    // === YÖNETİM ===
    TreatmentType updateStatus(String id, TreatmentStatus status);
    TreatmentType togglePopular(String id);
    TreatmentType toggleFeatured(String id);
    TreatmentType addFaq(String id, TreatmentType.FAQ faq);
    TreatmentType removeFaq(String id, int faqIndex);
    TreatmentType incrementViewCount(String id);
    TreatmentType incrementInquiryCount(String id);

    // === İSTATİSTİK ===
    long countAll();
    long countByCategory(TreatmentCategory category);
    long countActive();
    long countPopular();
}
