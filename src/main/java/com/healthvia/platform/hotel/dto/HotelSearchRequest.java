// hotel/dto/HotelSearchRequest.java
package com.healthvia.platform.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchRequest {

    // === KONUM FİLTRELERİ ===
    private String province;
    private String district;
    private String city; // province ile aynı, frontend uyumu için
    private String country;
    
    // === TARİH FİLTRELERİ ===
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    
    // === MİSAFİR BİLGİLERİ ===
    private Integer guests;
    private Integer rooms;
    private Integer adults;
    private Integer children;
    
    // === FİYAT FİLTRELERİ ===
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    
    // === DEĞERLENDİRME FİLTRELERİ ===
    private Double minRating;
    private Integer minStars;
    private Integer maxStars;
    
    // === SAĞLIK TURİZMİ FİLTRELERİ ===
    private Boolean isMedicalFocus;
    private Double maxDistanceToHospital; // km cinsinden
    private String partnerHospitalId;
    private Boolean hasMedicalStaff;
    private Boolean wheelchairAccessible;
    private Boolean hasPatientRooms;
    
    // === OLANAK FİLTRELERİ ===
    private List<String> amenities;
    private Boolean hasWifi;
    private Boolean hasPool;
    private Boolean hasSpa;
    private Boolean hasGym;
    private Boolean hasParking;
    private Boolean hasAirportTransfer;
    private Boolean hasHospitalTransfer;
    private Boolean hasRestaurant;
    private Boolean hasRoomService;
    private Boolean has24HourReception;
    
    // === KAHVALTI FİLTRESİ ===
    private Boolean breakfastIncluded;
    
    // === ÖNE ÇIKAN FİLTRELERİ ===
    private Boolean isFeatured;
    private Boolean isVerified;
    private Boolean isHealthviaPartner;
    
    // === SIRALAMA ===
    private SortBy sortBy;
    private SortDirection sortDirection;
    
    // === METİN ARAMA ===
    private String query; // Genel arama terimi
    private String hospitalName; // Hastane adına göre yakın oteller
    
    // === SAYFALAMA ===
    private Integer page;
    private Integer size;

    // === ENUMS ===
    public enum SortBy {
        PRICE,          // Fiyata göre
        RATING,         // Puana göre
        DISTANCE,       // Hastaneye uzaklığa göre
        STARS,          // Yıldıza göre
        REVIEW_COUNT,   // Yorum sayısına göre
        NAME,           // İsme göre
        CREATED_AT      // Oluşturulma tarihine göre
    }
    
    public enum SortDirection {
        ASC,
        DESC
    }

    // === HELPER METHODS ===
    
    /**
     * Konaklama gece sayısını hesapla
     */
    public Integer getNights() {
        if (checkInDate != null && checkOutDate != null) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        }
        return null;
    }
    
    /**
     * Varsayılan değerleri uygula
     */
    public HotelSearchRequest withDefaults() {
        if (this.page == null) this.page = 0;
        if (this.size == null) this.size = 20;
        if (this.sortBy == null) this.sortBy = SortBy.RATING;
        if (this.sortDirection == null) this.sortDirection = SortDirection.DESC;
        if (this.guests == null) this.guests = 2;
        if (this.rooms == null) this.rooms = 1;
        if (this.currency == null) this.currency = "USD";
        return this;
    }
    
    /**
     * Geçerli tarih aralığı kontrolü
     */
    public boolean hasValidDateRange() {
        if (checkInDate == null || checkOutDate == null) {
            return true; // Tarih belirtilmemişse sorun yok
        }
        return checkInDate.isBefore(checkOutDate) && 
               !checkInDate.isBefore(LocalDate.now());
    }
    
    /**
     * Fiyat aralığı kontrolü
     */
    public boolean hasValidPriceRange() {
        if (minPrice == null || maxPrice == null) {
            return true;
        }
        return minPrice.compareTo(maxPrice) <= 0;
    }
    
    /**
     * En az bir filtre uygulanmış mı?
     */
    public boolean hasAnyFilter() {
        return province != null || 
               district != null ||
               minPrice != null ||
               maxPrice != null ||
               minRating != null ||
               minStars != null ||
               isMedicalFocus != null ||
               (amenities != null && !amenities.isEmpty()) ||
               query != null;
    }

    /**
     * Sağlık turizmi filtresi uygulanmış mı?
     */
    public boolean hasMedicalFilters() {
        return isMedicalFocus != null ||
               maxDistanceToHospital != null ||
               partnerHospitalId != null ||
               hasMedicalStaff != null ||
               wheelchairAccessible != null ||
               hasPatientRooms != null;
    }
}
