// hotel/service/HotelService.java
package com.healthvia.platform.hotel.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.hotel.entity.Hotel;
import com.healthvia.platform.hotel.entity.Hotel.HotelStatus;

public interface HotelService {

    // === CRUD İŞLEMLERİ ===
    
    /**
     * Yeni otel oluştur
     */
    Hotel createHotel(Hotel hotel);
    
    /**
     * Otel güncelle
     */
    Hotel updateHotel(String id, Hotel hotel);
    
    /**
     * Otel bul (ID ile)
     */
    Optional<Hotel> findById(String id);
    
    /**
     * Otel sil (soft delete)
     */
    void deleteHotel(String id, String deletedBy);
    
    /**
     * Tüm otelleri listele (sayfalı)
     */
    Page<Hotel> findAll(Pageable pageable);
    
    /**
     * Aktif otelleri listele
     */
    Page<Hotel> findActiveHotels(Pageable pageable);

    // === ARAMA & FİLTRELEME ===
    
    /**
     * Otel ara (isim, konum, vb.)
     */
    Page<Hotel> searchHotels(String searchTerm, Pageable pageable);
    
    /**
     * Filtreli otel arama
     */
    Page<Hotel> searchHotelsWithFilters(
        String province,
        String district,
        Boolean isMedicalFocus,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Double minRating,
        Integer minStars,
        List<String> amenities,
        Pageable pageable
    );
    
    /**
     * İl bazlı otel listesi
     */
    Page<Hotel> findByProvince(String province, Pageable pageable);
    
    /**
     * Fiyat aralığına göre oteller
     */
    Page<Hotel> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    /**
     * Minimum puana göre oteller
     */
    Page<Hotel> findByMinRating(Double minRating, Pageable pageable);

    // === SAĞLIK TURİZMİ ===
    
    /**
     * Tıbbi odaklı oteller
     */
    List<Hotel> findMedicalFocusHotels();
    
    /**
     * Hastaneye yakın oteller
     */
    List<Hotel> findHotelsNearHospital(Double maxDistanceKm);
    
    /**
     * Partner hastane otelleri
     */
    List<Hotel> findByPartnerHospital(String hospitalId);
    
    /**
     * HealthVia partner oteller (indirimli)
     */
    List<Hotel> findHealthviaPartnerHotels();

    // === ÖNE ÇIKANLAR ===
    
    /**
     * Öne çıkan oteller
     */
    List<Hotel> findFeaturedHotels();
    
    /**
     * Öne çıkan tıbbi oteller
     */
    List<Hotel> findFeaturedMedicalHotels();
    
    /**
     * En yüksek puanlı oteller
     */
    List<Hotel> findTopRatedHotels(int limit);
    
    /**
     * Doğrulanmış oteller
     */
    List<Hotel> findVerifiedHotels();

    // === OLANAK YÖNETİMİ ===
    
    /**
     * Belirli olanaklara sahip oteller
     */
    List<Hotel> findByAmenities(List<String> amenities);
    
    /**
     * Otel olanağı ekle
     */
    Hotel addAmenity(String hotelId, String amenity);
    
    /**
     * Otel olanağı kaldır
     */
    Hotel removeAmenity(String hotelId, String amenity);

    // === ODA YÖNETİMİ ===
    
    /**
     * Oda tipi ekle
     */
    Hotel addRoomType(String hotelId, Hotel.RoomType roomType);
    
    /**
     * Oda tipini güncelle
     */
    Hotel updateRoomType(String hotelId, String roomTypeName, Hotel.RoomType roomType);
    
    /**
     * Oda tipini kaldır
     */
    Hotel removeRoomType(String hotelId, String roomTypeName);
    
    /**
     * Müsait oda sayısını güncelle
     */
    Hotel updateAvailableRooms(String hotelId, Integer availableRooms);

    // === DURUM YÖNETİMİ ===
    
    /**
     * Otel durumunu güncelle
     */
    Hotel updateStatus(String hotelId, HotelStatus status, String updatedBy);
    
    /**
     * Oteli öne çıkar/çıkarma
     */
    Hotel toggleFeatured(String hotelId);
    
    /**
     * Oteli doğrula
     */
    Hotel verifyHotel(String hotelId, String verifiedBy);

    // === DEĞERLENDİRME ===
    
    /**
     * Otel puanını güncelle
     */
    Hotel updateRating(String hotelId, Double newRating, Integer reviewCount);
    
    /**
     * Alt puanları güncelle (temizlik, hizmet, konum, değer)
     */
    Hotel updateSubRatings(String hotelId, Double cleanliness, Double service, 
                          Double location, Double value);

    // === İSTATİSTİKLER ===
    
    /**
     * İl bazlı otel sayısı
     */
    long countByProvince(String province);
    
    /**
     * Tıbbi odaklı otel sayısı
     */
    long countMedicalFocusHotels();
    
    /**
     * Toplam aktif otel sayısı
     */
    long countActiveHotels();
}
