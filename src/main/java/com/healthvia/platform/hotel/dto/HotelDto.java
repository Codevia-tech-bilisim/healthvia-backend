// hotel/dto/HotelDto.java
package com.healthvia.platform.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.hotel.entity.Hotel;
import com.healthvia.platform.hotel.entity.Hotel.HotelStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelDto {

    private String id;
    private String name;
    private String description;
    private Integer starRating;
    
    // Konum
    private String province;
    private String district;
    private String address;
    private String fullAddress;
    private String country;
    private Double latitude;
    private Double longitude;
    
    // Sağlık Turizmi
    private Boolean isMedicalFocus;
    private Double distanceToHospitalKm;
    private List<String> partnerHospitals;
    private Set<String> medicalServices;
    private Boolean hasMedicalStaff;
    private Boolean wheelchairAccessible;
    private Boolean hasPatientRooms;
    
    // Fiyatlandırma
    private BigDecimal pricePerNight;
    private BigDecimal discountedPrice;
    private String currency;
    private Boolean priceIncludesBreakfast;
    private Integer discountPercentage;
    private Integer healthviaPartnerDiscount;
    
    // Olanaklar
    private Set<String> amenities;
    private Set<String> roomAmenities;
    private Boolean hasAirportTransfer;
    private Boolean hasHospitalTransfer;
    private Boolean has24HourReception;
    private Boolean hasRestaurant;
    private Boolean hasRoomService;
    
    // Değerlendirme
    private Double rating;
    private Integer reviewCount;
    private Double cleanlinessRating;
    private Double serviceRating;
    private Double locationRating;
    private Double valueRating;
    
    // Oda Bilgileri
    private Integer totalRooms;
    private Integer availableRooms;
    private List<RoomTypeDto> roomTypes;
    
    // İletişim
    private String phone;
    private String email;
    private String website;
    private String whatsappNumber;
    
    // Görseller
    private String mainImageUrl;
    private List<String> imageUrls;
    private String virtualTourUrl;
    
    // Politikalar
    private String checkInTime;
    private String checkOutTime;
    private String cancellationPolicy;
    
    // Durum
    private HotelStatus status;
    private Boolean isFeatured;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    
    // Meta
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === Inner Classes ===
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeDto {
        private String name;
        private String description;
        private Integer capacity;
        private BigDecimal pricePerNight;
        private Set<String> amenities;
        private Integer availableCount;
        private List<String> imageUrls;
        private Boolean isMedicalRoom;
    }

    // === Factory Method ===
    
    public static HotelDto fromEntity(Hotel hotel) {
        if (hotel == null) return null;
        
        HotelDtoBuilder builder = HotelDto.builder()
            .id(hotel.getId())
            .name(hotel.getName())
            .description(hotel.getDescription())
            .starRating(hotel.getStarRating())
            // Konum
            .province(hotel.getProvince())
            .district(hotel.getDistrict())
            .address(hotel.getAddress())
            .fullAddress(hotel.getFullAddress())
            .country(hotel.getCountry())
            .latitude(hotel.getLatitude())
            .longitude(hotel.getLongitude())
            // Sağlık Turizmi
            .isMedicalFocus(hotel.getIsMedicalFocus())
            .distanceToHospitalKm(hotel.getDistanceToHospitalKm())
            .partnerHospitals(hotel.getPartnerHospitals())
            .medicalServices(hotel.getMedicalServices())
            .hasMedicalStaff(hotel.getHasMedicalStaff())
            .wheelchairAccessible(hotel.getWheelchairAccessible())
            .hasPatientRooms(hotel.getHasPatientRooms())
            // Fiyatlandırma
            .pricePerNight(hotel.getPricePerNight())
            .discountedPrice(hotel.getDiscountedPrice())
            .currency(hotel.getCurrency())
            .priceIncludesBreakfast(hotel.getPriceIncludesBreakfast())
            .discountPercentage(hotel.getDiscountPercentage())
            .healthviaPartnerDiscount(hotel.getHealthviaPartnerDiscount())
            // Olanaklar
            .amenities(hotel.getAmenities())
            .roomAmenities(hotel.getRoomAmenities())
            .hasAirportTransfer(hotel.getHasAirportTransfer())
            .hasHospitalTransfer(hotel.getHasHospitalTransfer())
            .has24HourReception(hotel.getHas24HourReception())
            .hasRestaurant(hotel.getHasRestaurant())
            .hasRoomService(hotel.getHasRoomService())
            // Değerlendirme
            .rating(hotel.getRating())
            .reviewCount(hotel.getReviewCount())
            .cleanlinessRating(hotel.getCleanlinessRating())
            .serviceRating(hotel.getServiceRating())
            .locationRating(hotel.getLocationRating())
            .valueRating(hotel.getValueRating())
            // Oda Bilgileri
            .totalRooms(hotel.getTotalRooms())
            .availableRooms(hotel.getAvailableRooms())
            // İletişim
            .phone(hotel.getPhone())
            .email(hotel.getEmail())
            .website(hotel.getWebsite())
            .whatsappNumber(hotel.getWhatsappNumber())
            // Görseller
            .mainImageUrl(hotel.getMainImageUrl())
            .imageUrls(hotel.getImageUrls())
            .virtualTourUrl(hotel.getVirtualTourUrl())
            // Politikalar
            .checkInTime(hotel.getCheckInTime())
            .checkOutTime(hotel.getCheckOutTime())
            .cancellationPolicy(hotel.getCancellationPolicy())
            // Durum
            .status(hotel.getStatus())
            .isFeatured(hotel.getIsFeatured())
            .isVerified(hotel.getIsVerified())
            .verifiedAt(hotel.getVerifiedAt())
            // Meta
            .createdAt(hotel.getCreatedAt())
            .updatedAt(hotel.getUpdatedAt());
        
        // Oda tiplerini dönüştür
        if (hotel.getRoomTypes() != null) {
            List<RoomTypeDto> roomTypeDtos = hotel.getRoomTypes().stream()
                .map(rt -> RoomTypeDto.builder()
                    .name(rt.getName())
                    .description(rt.getDescription())
                    .capacity(rt.getCapacity())
                    .pricePerNight(rt.getPricePerNight())
                    .amenities(rt.getAmenities())
                    .availableCount(rt.getAvailableCount())
                    .imageUrls(rt.getImageUrls())
                    .isMedicalRoom(rt.getIsMedicalRoom())
                    .build())
                .toList();
            builder.roomTypes(roomTypeDtos);
        }
        
        return builder.build();
    }
}
