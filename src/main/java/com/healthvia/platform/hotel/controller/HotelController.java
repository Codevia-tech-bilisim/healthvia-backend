// hotel/controller/HotelController.java
package com.healthvia.platform.hotel.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.hotel.dto.HotelCreateRequest;
import com.healthvia.platform.hotel.dto.HotelDto;
import com.healthvia.platform.hotel.entity.Hotel;
import com.healthvia.platform.hotel.entity.Hotel.HotelStatus;
import com.healthvia.platform.hotel.service.HotelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {

    private final HotelService hotelService;

    // ===================================================================
    // PUBLIC ENDPOINTS - Herkes erişebilir
    // ===================================================================

    /**
     * Aktif otelleri listele (sayfalı)
     */
    @GetMapping("/public")
    public ApiResponse<Page<HotelDto>> getActiveHotels(
            @PageableDefault(size = 20, sort = "rating,desc") Pageable pageable) {
        
        log.info("Getting active hotels list");
        Page<Hotel> hotels = hotelService.findActiveHotels(pageable);
        Page<HotelDto> hotelDtos = hotels.map(HotelDto::fromEntity);
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Otel ara (filtreli)
     */
    @GetMapping("/public/search")
    public ApiResponse<Page<HotelDto>> searchHotels(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Boolean isMedicalFocus,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minStars,
            @RequestParam(required = false) List<String> amenities,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Searching hotels with query: {}, province: {}, medicalFocus: {}", 
                 query, province, isMedicalFocus);
        
        Page<Hotel> hotels;
        
        if (query != null && !query.isEmpty()) {
            hotels = hotelService.searchHotels(query, pageable);
        } else {
            hotels = hotelService.searchHotelsWithFilters(
                province, district, isMedicalFocus, minPrice, maxPrice, 
                minRating, minStars, amenities, pageable);
        }
        
        Page<HotelDto> hotelDtos = hotels.map(HotelDto::fromEntity);
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Otel detayı
     */
    @GetMapping("/public/{id}")
    public ApiResponse<HotelDto> getHotelById(@PathVariable String id) {
        log.info("Getting hotel by ID: {}", id);
        
        Hotel hotel = hotelService.findById(id)
            .orElseThrow(() -> new RuntimeException("Otel bulunamadı: " + id));
        
        return ApiResponse.success(HotelDto.fromEntity(hotel));
    }

    /**
     * İl bazlı oteller
     */
    @GetMapping("/public/by-province/{province}")
    public ApiResponse<Page<HotelDto>> getHotelsByProvince(
            @PathVariable String province,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Hotel> hotels = hotelService.findByProvince(province, pageable);
        Page<HotelDto> hotelDtos = hotels.map(HotelDto::fromEntity);
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Tıbbi odaklı oteller
     */
    @GetMapping("/public/medical")
    public ApiResponse<List<HotelDto>> getMedicalFocusHotels() {
        List<Hotel> hotels = hotelService.findMedicalFocusHotels();
        List<HotelDto> hotelDtos = hotels.stream()
            .map(HotelDto::fromEntity)
            .toList();
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Hastaneye yakın oteller
     */
    @GetMapping("/public/near-hospital")
    public ApiResponse<List<HotelDto>> getHotelsNearHospital(
            @RequestParam(defaultValue = "3.0") Double maxDistanceKm) {
        
        List<Hotel> hotels = hotelService.findHotelsNearHospital(maxDistanceKm);
        List<HotelDto> hotelDtos = hotels.stream()
            .map(HotelDto::fromEntity)
            .toList();
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Öne çıkan oteller
     */
    @GetMapping("/public/featured")
    public ApiResponse<List<HotelDto>> getFeaturedHotels() {
        List<Hotel> hotels = hotelService.findFeaturedHotels();
        List<HotelDto> hotelDtos = hotels.stream()
            .map(HotelDto::fromEntity)
            .toList();
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Öne çıkan tıbbi oteller
     */
    @GetMapping("/public/featured/medical")
    public ApiResponse<List<HotelDto>> getFeaturedMedicalHotels() {
        List<Hotel> hotels = hotelService.findFeaturedMedicalHotels();
        List<HotelDto> hotelDtos = hotels.stream()
            .map(HotelDto::fromEntity)
            .toList();
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * En yüksek puanlı oteller
     */
    @GetMapping("/public/top-rated")
    public ApiResponse<List<HotelDto>> getTopRatedHotels(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Hotel> hotels = hotelService.findTopRatedHotels(limit);
        List<HotelDto> hotelDtos = hotels.stream()
            .map(HotelDto::fromEntity)
            .toList();
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * HealthVia partner oteller (indirimli)
     */
    @GetMapping("/public/partners")
    public ApiResponse<List<HotelDto>> getHealthviaPartnerHotels() {
        List<Hotel> hotels = hotelService.findHealthviaPartnerHotels();
        List<HotelDto> hotelDtos = hotels.stream()
            .map(HotelDto::fromEntity)
            .toList();
        
        return ApiResponse.success(hotelDtos);
    }

    // ===================================================================
    // ADMIN ENDPOINTS - Sadece adminler erişebilir
    // ===================================================================

    /**
     * Tüm otelleri listele (sayfalı) - Admin
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<HotelDto>> getAllHotels(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Hotel> hotels = hotelService.findAll(pageable);
        Page<HotelDto> hotelDtos = hotels.map(HotelDto::fromEntity);
        
        return ApiResponse.success(hotelDtos);
    }

    /**
     * Yeni otel oluştur - Admin
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> createHotel(@Valid @RequestBody HotelCreateRequest request) {
        log.info("Creating new hotel: {}", request.getName());
        
        Hotel hotel = request.toEntity();
        Hotel createdHotel = hotelService.createHotel(hotel);
        
        return ApiResponse.success(HotelDto.fromEntity(createdHotel), "Otel başarıyla oluşturuldu");
    }

    /**
     * Otel güncelle - Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> updateHotel(
            @PathVariable String id,
            @Valid @RequestBody HotelCreateRequest request) {
        
        log.info("Updating hotel: {}", id);
        
        Hotel hotel = request.toEntity();
        Hotel updatedHotel = hotelService.updateHotel(id, hotel);
        
        return ApiResponse.success(HotelDto.fromEntity(updatedHotel), "Otel başarıyla güncellendi");
    }

    /**
     * Otel sil (soft delete) - Admin
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteHotel(@PathVariable String id) {
        log.info("Deleting hotel: {}", id);
        
        String deletedBy = SecurityUtils.getCurrentUserId();
        hotelService.deleteHotel(id, deletedBy);
        
        return ApiResponse.success(null, "Otel başarıyla silindi");
    }

    /**
     * Otel durumunu güncelle - Admin
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> updateHotelStatus(
            @PathVariable String id,
            @RequestParam HotelStatus status) {
        
        log.info("Updating hotel {} status to {}", id, status);
        
        String updatedBy = SecurityUtils.getCurrentUserId();
        Hotel hotel = hotelService.updateStatus(id, status, updatedBy);
        
        return ApiResponse.success(HotelDto.fromEntity(hotel), "Otel durumu güncellendi");
    }

    /**
     * Oteli öne çıkar/çıkarma - Admin
     */
    @PatchMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> toggleFeatured(@PathVariable String id) {
        log.info("Toggling featured status for hotel: {}", id);
        
        Hotel hotel = hotelService.toggleFeatured(id);
        String message = hotel.getIsFeatured() ? "Otel öne çıkarıldı" : "Otel öne çıkarmadan kaldırıldı";
        
        return ApiResponse.success(HotelDto.fromEntity(hotel), message);
    }

    /**
     * Oteli doğrula - Admin
     */
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> verifyHotel(@PathVariable String id) {
        log.info("Verifying hotel: {}", id);
        
        String verifiedBy = SecurityUtils.getCurrentUserId();
        Hotel hotel = hotelService.verifyHotel(id, verifiedBy);
        
        return ApiResponse.success(HotelDto.fromEntity(hotel), "Otel doğrulandı");
    }

    /**
     * Otel puanını güncelle - Admin
     */
    @PatchMapping("/{id}/rating")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> updateRating(
            @PathVariable String id,
            @RequestParam Double rating,
            @RequestParam Integer reviewCount) {
        
        Hotel hotel = hotelService.updateRating(id, rating, reviewCount);
        return ApiResponse.success(HotelDto.fromEntity(hotel), "Puan güncellendi");
    }

    /**
     * Oda tipi ekle - Admin
     */
    @PostMapping("/{id}/room-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> addRoomType(
            @PathVariable String id,
            @Valid @RequestBody Hotel.RoomType roomType) {
        
        Hotel hotel = hotelService.addRoomType(id, roomType);
        return ApiResponse.success(HotelDto.fromEntity(hotel), "Oda tipi eklendi");
    }

    /**
     * Müsait oda sayısını güncelle - Admin
     */
    @PatchMapping("/{id}/available-rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelDto> updateAvailableRooms(
            @PathVariable String id,
            @RequestParam Integer availableRooms) {
        
        Hotel hotel = hotelService.updateAvailableRooms(id, availableRooms);
        return ApiResponse.success(HotelDto.fromEntity(hotel), "Müsait oda sayısı güncellendi");
    }

    // ===================================================================
    // STATISTICS ENDPOINTS - Admin
    // ===================================================================

    /**
     * İstatistikler - Admin
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HotelStatistics> getStatistics() {
        HotelStatistics stats = new HotelStatistics();
        stats.setTotalActiveHotels(hotelService.countActiveHotels());
        stats.setTotalMedicalFocusHotels(hotelService.countMedicalFocusHotels());
        
        return ApiResponse.success(stats);
    }

    // === Inner Classes ===
    
    @lombok.Data
    public static class HotelStatistics {
        private long totalActiveHotels;
        private long totalMedicalFocusHotels;
        private long totalPartnerHotels;
        private long totalVerifiedHotels;
    }
}
