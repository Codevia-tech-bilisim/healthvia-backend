// hotel/repository/HotelRepository.java
package com.healthvia.platform.hotel.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.hotel.entity.Hotel;
import com.healthvia.platform.hotel.entity.Hotel.HotelStatus;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String> {

    // === TEMEL SORGULAR ===
    
    Optional<Hotel> findByIdAndDeletedFalse(String id);
    
    List<Hotel> findByStatusAndDeletedFalse(HotelStatus status);
    
    Page<Hotel> findByDeletedFalse(Pageable pageable);
    
    Page<Hotel> findByStatusAndDeletedFalse(HotelStatus status, Pageable pageable);

    // === KONUM BAZLI SORGULAR ===
    
    List<Hotel> findByProvinceAndDeletedFalse(String province);
    
    List<Hotel> findByProvinceAndDistrictAndDeletedFalse(String province, String district);
    
    Page<Hotel> findByProvinceAndStatusAndDeletedFalse(String province, HotelStatus status, Pageable pageable);
    
    @Query("{ 'province': ?0, 'status': 'ACTIVE', 'deleted': false }")
    Page<Hotel> findActiveHotelsByProvince(String province, Pageable pageable);

    // === SAĞLIK TURİZMİ SORGULARI ===
    
    List<Hotel> findByIsMedicalFocusTrueAndDeletedFalse();
    
    Page<Hotel> findByIsMedicalFocusTrueAndStatusAndDeletedFalse(HotelStatus status, Pageable pageable);
    
    @Query("{ 'isMedicalFocus': true, 'distanceToHospitalKm': { $lte: ?0 }, 'status': 'ACTIVE', 'deleted': false }")
    List<Hotel> findMedicalHotelsNearHospital(Double maxDistanceKm);
    
    @Query("{ 'partnerHospitals': ?0, 'status': 'ACTIVE', 'deleted': false }")
    List<Hotel> findByPartnerHospital(String hospitalId);

    // === FİYAT BAZLI SORGULAR ===
    
    @Query("{ 'pricePerNight': { $gte: ?0, $lte: ?1 }, 'status': 'ACTIVE', 'deleted': false }")
    Page<Hotel> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    @Query("{ 'pricePerNight': { $lte: ?0 }, 'province': ?1, 'status': 'ACTIVE', 'deleted': false }")
    Page<Hotel> findByMaxPriceAndProvince(BigDecimal maxPrice, String province, Pageable pageable);

    // === DEĞERLENDİRME BAZLI SORGULAR ===
    
    @Query("{ 'rating': { $gte: ?0 }, 'status': 'ACTIVE', 'deleted': false }")
    Page<Hotel> findByMinRating(Double minRating, Pageable pageable);
    
    @Query("{ 'rating': { $gte: ?0 }, 'province': ?1, 'status': 'ACTIVE', 'deleted': false }")
    Page<Hotel> findByMinRatingAndProvince(Double minRating, String province, Pageable pageable);
    
    List<Hotel> findTop10ByStatusAndDeletedFalseOrderByRatingDesc(HotelStatus status);

    // === OLANAK BAZLI SORGULAR ===
    
    @Query("{ 'amenities': { $all: ?0 }, 'status': 'ACTIVE', 'deleted': false }")
    List<Hotel> findByAmenities(List<String> amenities);
    
    @Query("{ 'amenities': ?0, 'status': 'ACTIVE', 'deleted': false }")
    List<Hotel> findByAmenity(String amenity);

    // === ÖNE ÇIKAN & DOĞRULANMIŞ ===
    
    List<Hotel> findByIsFeaturedTrueAndStatusAndDeletedFalse(HotelStatus status);
    
    List<Hotel> findByIsVerifiedTrueAndStatusAndDeletedFalse(HotelStatus status);
    
    @Query("{ 'isFeatured': true, 'isMedicalFocus': true, 'status': 'ACTIVE', 'deleted': false }")
    List<Hotel> findFeaturedMedicalHotels();

    // === ARAMA ===
    
    @Query("{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'province': { $regex: ?0, $options: 'i' } }, " +
           "{ 'district': { $regex: ?0, $options: 'i' } }, " +
           "{ 'address': { $regex: ?0, $options: 'i' } } " +
           "], 'status': 'ACTIVE', 'deleted': false }")
    Page<Hotel> searchHotels(String searchTerm, Pageable pageable);

    // === GELİŞMİŞ FİLTRELEME ===
    
    @Query("{ " +
           "$and: [ " +
           "  { 'status': 'ACTIVE' }, " +
           "  { 'deleted': false }, " +
           "  { $or: [ { 'province': { $exists: false } }, { 'province': ?0 } ] }, " +
           "  { $or: [ { 'isMedicalFocus': { $exists: false } }, { 'isMedicalFocus': ?1 } ] }, " +
           "  { 'pricePerNight': { $lte: ?2 } }, " +
           "  { 'rating': { $gte: ?3 } } " +
           "] }")
    Page<Hotel> findWithFilters(String province, Boolean isMedicalFocus, 
                                BigDecimal maxPrice, Double minRating, Pageable pageable);

    // === İSTATİSTİKLER ===
    
    long countByStatusAndDeletedFalse(HotelStatus status);
    
    long countByProvinceAndStatusAndDeletedFalse(String province, HotelStatus status);
    
    long countByIsMedicalFocusTrueAndStatusAndDeletedFalse(HotelStatus status);

    // === HEALTHVIA PARTNER ===
    
    @Query("{ 'healthviaPartnerDiscount': { $gt: 0 }, 'status': 'ACTIVE', 'deleted': false }")
    List<Hotel> findHealthviaPartnerHotels();
}
