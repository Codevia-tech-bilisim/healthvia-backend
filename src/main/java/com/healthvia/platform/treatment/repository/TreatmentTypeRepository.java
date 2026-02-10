// treatment/repository/TreatmentTypeRepository.java
package com.healthvia.platform.treatment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.treatment.entity.TreatmentType;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentCategory;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentStatus;

@Repository
public interface TreatmentTypeRepository extends MongoRepository<TreatmentType, String> {

    // === TEMEL SORGULAR ===

    Optional<TreatmentType> findBySlug(String slug);

    Optional<TreatmentType> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    // === KATEGORİ BAZLI ===

    List<TreatmentType> findByCategoryAndDeletedFalse(TreatmentCategory category);

    List<TreatmentType> findByCategoryAndStatusAndDeletedFalse(TreatmentCategory category, TreatmentStatus status);

    long countByCategoryAndDeletedFalse(TreatmentCategory category);

    // === DURUM BAZLI ===

    List<TreatmentType> findByStatusAndDeletedFalseOrderBySortOrderAsc(TreatmentStatus status);

    Page<TreatmentType> findByStatusAndDeletedFalse(TreatmentStatus status, Pageable pageable);

    long countByStatusAndDeletedFalse(TreatmentStatus status);

    // === POPÜLER & ÖNE ÇIKAN ===

    List<TreatmentType> findByIsPopularTrueAndStatusAndDeletedFalseOrderBySortOrderAsc(TreatmentStatus status);

    List<TreatmentType> findByIsFeaturedTrueAndStatusAndDeletedFalseOrderBySortOrderAsc(TreatmentStatus status);

    // === ARAMA ===

    @Query("{ $or: [ " +
           "{'name': {$regex: ?0, $options: 'i'}}, " +
           "{'nameTr': {$regex: ?0, $options: 'i'}}, " +
           "{'summary': {$regex: ?0, $options: 'i'}}, " +
           "{'summaryTr': {$regex: ?0, $options: 'i'}}, " +
           "{'subCategory': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<TreatmentType> searchByKeyword(String keyword, Pageable pageable);

    // === İLİŞKİ BAZLI ===

    @Query("{ 'availableHospitalIds': ?0, 'deleted': false, 'status': 'ACTIVE' }")
    List<TreatmentType> findByHospitalId(String hospitalId);

    @Query("{ 'specialistDoctorIds': ?0, 'deleted': false, 'status': 'ACTIVE' }")
    List<TreatmentType> findByDoctorId(String doctorId);

    // === FİYAT BAZLI ===

    @Query("{ 'basePrice': { $gte: ?0, $lte: ?1 }, 'deleted': false, 'status': 'ACTIVE' }")
    List<TreatmentType> findByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    // === İSTATİSTİK ===

    long countByDeletedFalse();

    long countByIsPopularTrueAndDeletedFalse();
}
