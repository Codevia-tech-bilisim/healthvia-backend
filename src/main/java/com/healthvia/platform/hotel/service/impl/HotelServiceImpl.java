// hotel/service/impl/HotelServiceImpl.java
package com.healthvia.platform.hotel.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.hotel.entity.Hotel;
import com.healthvia.platform.hotel.entity.Hotel.HotelStatus;
import com.healthvia.platform.hotel.repository.HotelRepository;
import com.healthvia.platform.hotel.service.HotelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;

    // === CRUD İŞLEMLERİ ===

    @Override
    public Hotel createHotel(Hotel hotel) {
        log.info("Creating new hotel: {}", hotel.getName());
        
        // Varsayılan değerleri ayarla
        if (hotel.getStatus() == null) {
            hotel.setStatus(HotelStatus.PENDING_REVIEW);
        }
        if (hotel.getRating() == null) {
            hotel.setRating(0.0);
        }
        if (hotel.getReviewCount() == null) {
            hotel.setReviewCount(0);
        }
        
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel updateHotel(String id, Hotel hotelData) {
        log.info("Updating hotel: {}", id);
        
        Hotel existingHotel = findByIdOrThrow(id);
        
        // Güncellenebilir alanları kopyala
        if (hotelData.getName() != null) existingHotel.setName(hotelData.getName());
        if (hotelData.getDescription() != null) existingHotel.setDescription(hotelData.getDescription());
        if (hotelData.getStarRating() != null) existingHotel.setStarRating(hotelData.getStarRating());
        if (hotelData.getProvince() != null) existingHotel.setProvince(hotelData.getProvince());
        if (hotelData.getDistrict() != null) existingHotel.setDistrict(hotelData.getDistrict());
        if (hotelData.getAddress() != null) existingHotel.setAddress(hotelData.getAddress());
        if (hotelData.getPricePerNight() != null) existingHotel.setPricePerNight(hotelData.getPricePerNight());
        if (hotelData.getAmenities() != null) existingHotel.setAmenities(hotelData.getAmenities());
        if (hotelData.getIsMedicalFocus() != null) existingHotel.setIsMedicalFocus(hotelData.getIsMedicalFocus());
        if (hotelData.getDistanceToHospitalKm() != null) existingHotel.setDistanceToHospitalKm(hotelData.getDistanceToHospitalKm());
        if (hotelData.getMainImageUrl() != null) existingHotel.setMainImageUrl(hotelData.getMainImageUrl());
        if (hotelData.getImageUrls() != null) existingHotel.setImageUrls(hotelData.getImageUrls());
        if (hotelData.getPhone() != null) existingHotel.setPhone(hotelData.getPhone());
        if (hotelData.getEmail() != null) existingHotel.setEmail(hotelData.getEmail());
        
        return hotelRepository.save(existingHotel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Hotel> findById(String id) {
        return hotelRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public void deleteHotel(String id, String deletedBy) {
        log.info("Soft deleting hotel: {} by user: {}", id, deletedBy);
        
        Hotel hotel = findByIdOrThrow(id);
        hotel.setDeleted(true);
        hotel.setDeletedAt(LocalDateTime.now());
        hotel.setDeletedBy(deletedBy);
        
        hotelRepository.save(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> findAll(Pageable pageable) {
        return hotelRepository.findByDeletedFalse(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> findActiveHotels(Pageable pageable) {
        return hotelRepository.findByStatusAndDeletedFalse(HotelStatus.ACTIVE, pageable);
    }

    // === ARAMA & FİLTRELEME ===

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> searchHotels(String searchTerm, Pageable pageable) {
        log.debug("Searching hotels with term: {}", searchTerm);
        return hotelRepository.searchHotels(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> searchHotelsWithFilters(
            String province,
            String district,
            Boolean isMedicalFocus,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Integer minStars,
            List<String> amenities,
            Pageable pageable) {
        
        log.debug("Searching hotels with filters - province: {}, medicalFocus: {}, priceRange: {}-{}", 
                  province, isMedicalFocus, minPrice, maxPrice);
        
        // Karmaşık filtreler için custom query kullan
        BigDecimal effectiveMaxPrice = maxPrice != null ? maxPrice : BigDecimal.valueOf(999999);
        Double effectiveMinRating = minRating != null ? minRating : 0.0;
        
        return hotelRepository.findWithFilters(province, isMedicalFocus, effectiveMaxPrice, effectiveMinRating, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> findByProvince(String province, Pageable pageable) {
        return hotelRepository.findActiveHotelsByProvince(province, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return hotelRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> findByMinRating(Double minRating, Pageable pageable) {
        return hotelRepository.findByMinRating(minRating, pageable);
    }

    // === SAĞLIK TURİZMİ ===

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findMedicalFocusHotels() {
        return hotelRepository.findByIsMedicalFocusTrueAndDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findHotelsNearHospital(Double maxDistanceKm) {
        return hotelRepository.findMedicalHotelsNearHospital(maxDistanceKm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findByPartnerHospital(String hospitalId) {
        return hotelRepository.findByPartnerHospital(hospitalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findHealthviaPartnerHotels() {
        return hotelRepository.findHealthviaPartnerHotels();
    }

    // === ÖNE ÇIKANLAR ===

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findFeaturedHotels() {
        return hotelRepository.findByIsFeaturedTrueAndStatusAndDeletedFalse(HotelStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findFeaturedMedicalHotels() {
        return hotelRepository.findFeaturedMedicalHotels();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findTopRatedHotels(int limit) {
        return hotelRepository.findTop10ByStatusAndDeletedFalseOrderByRatingDesc(HotelStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findVerifiedHotels() {
        return hotelRepository.findByIsVerifiedTrueAndStatusAndDeletedFalse(HotelStatus.ACTIVE);
    }

    // === OLANAK YÖNETİMİ ===

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findByAmenities(List<String> amenities) {
        return hotelRepository.findByAmenities(amenities);
    }

    @Override
    public Hotel addAmenity(String hotelId, String amenity) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.getAmenities().add(amenity);
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel removeAmenity(String hotelId, String amenity) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.getAmenities().remove(amenity);
        return hotelRepository.save(hotel);
    }

    // === ODA YÖNETİMİ ===

    @Override
    public Hotel addRoomType(String hotelId, Hotel.RoomType roomType) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.getRoomTypes().add(roomType);
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel updateRoomType(String hotelId, String roomTypeName, Hotel.RoomType updatedRoomType) {
        Hotel hotel = findByIdOrThrow(hotelId);
        
        hotel.getRoomTypes().removeIf(rt -> rt.getName().equals(roomTypeName));
        hotel.getRoomTypes().add(updatedRoomType);
        
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel removeRoomType(String hotelId, String roomTypeName) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.getRoomTypes().removeIf(rt -> rt.getName().equals(roomTypeName));
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel updateAvailableRooms(String hotelId, Integer availableRooms) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.setAvailableRooms(availableRooms);
        return hotelRepository.save(hotel);
    }

    // === DURUM YÖNETİMİ ===

    @Override
    public Hotel updateStatus(String hotelId, HotelStatus status, String updatedBy) {
        log.info("Updating hotel {} status to {}", hotelId, status);
        
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.setStatus(status);
        
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel toggleFeatured(String hotelId) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.setIsFeatured(!hotel.getIsFeatured());
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel verifyHotel(String hotelId, String verifiedBy) {
        log.info("Verifying hotel {} by {}", hotelId, verifiedBy);
        
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.setIsVerified(true);
        hotel.setVerifiedAt(LocalDateTime.now());
        
        return hotelRepository.save(hotel);
    }

    // === DEĞERLENDİRME ===

    @Override
    public Hotel updateRating(String hotelId, Double newRating, Integer reviewCount) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.setRating(newRating);
        hotel.setReviewCount(reviewCount);
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel updateSubRatings(String hotelId, Double cleanliness, Double service, 
                                  Double location, Double value) {
        Hotel hotel = findByIdOrThrow(hotelId);
        hotel.setCleanlinessRating(cleanliness);
        hotel.setServiceRating(service);
        hotel.setLocationRating(location);
        hotel.setValueRating(value);
        
        // Ortalama puanı hesapla
        double avg = (cleanliness + service + location + value) / 4.0;
        hotel.setRating(Math.round(avg * 10.0) / 10.0);
        
        return hotelRepository.save(hotel);
    }

    // === İSTATİSTİKLER ===

    @Override
    @Transactional(readOnly = true)
    public long countByProvince(String province) {
        return hotelRepository.countByProvinceAndStatusAndDeletedFalse(province, HotelStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMedicalFocusHotels() {
        return hotelRepository.countByIsMedicalFocusTrueAndStatusAndDeletedFalse(HotelStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveHotels() {
        return hotelRepository.countByStatusAndDeletedFalse(HotelStatus.ACTIVE);
    }

    // === HELPER METHODS ===

    private Hotel findByIdOrThrow(String id) {
        return hotelRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
    }
}
