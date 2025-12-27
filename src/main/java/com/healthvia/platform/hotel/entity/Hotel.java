// hotel/entity/Hotel.java
package com.healthvia.platform.hotel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.model.BaseEntity;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "hotels")
@CompoundIndex(def = "{'province': 1, 'isMedicalFocus': 1}")
@CompoundIndex(def = "{'rating': -1, 'reviewCount': -1}")
public class Hotel extends BaseEntity {

    // === TEMEL BİLGİLER ===
    @NotBlank(message = "Otel adı zorunludur")
    @Size(min = 2, max = 200, message = "Otel adı 2-200 karakter arasında olmalıdır")
    @Indexed
    private String name;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    @Field("star_rating")
    @Min(value = 1, message = "Yıldız en az 1 olmalıdır")
    @Max(value = 5, message = "Yıldız en fazla 5 olabilir")
    private Integer starRating; // 1-5 yıldız

    // === KONUM BİLGİLERİ ===
    @NotBlank(message = "İl zorunludur")
    @Indexed
    private String province;

    private String district;

    @NotBlank(message = "Adres zorunludur")
    private String address;

    @Field("postal_code")
    private String postalCode;

    private String country = "Turkey";

    // Koordinatlar (harita için)
    private Double latitude;
    private Double longitude;

    // === SAĞLIK TURİZMİ ÖZELLİKLERİ ===
    @Field("is_medical_focus")
    @Indexed
    private Boolean isMedicalFocus = false;

    @Field("distance_to_hospital_km")
    @DecimalMin(value = "0.0", message = "Mesafe negatif olamaz")
    private Double distanceToHospitalKm;

    @Field("partner_hospitals")
    private List<String> partnerHospitals; // İş birliği yapılan hastaneler

    @Field("medical_services")
    private Set<String> medicalServices; // Tıbbi hizmetler (hasta asistanı, tıbbi araç-gereç vb.)

    @Field("has_medical_staff")
    private Boolean hasMedicalStaff = false; // Yerinde sağlık personeli var mı

    @Field("wheelchair_accessible")
    private Boolean wheelchairAccessible = false;

    @Field("has_patient_rooms")
    private Boolean hasPatientRooms = false; // Hasta odaları var mı

    // === FİYATLANDIRMA ===
    @NotNull(message = "Gecelik fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    @Field("price_per_night")
    private BigDecimal pricePerNight;

    @Field("currency")
    private String currency = "USD";

    @Field("price_includes_breakfast")
    private Boolean priceIncludesBreakfast = false;

    @Field("discount_percentage")
    @Min(value = 0) @Max(value = 100)
    private Integer discountPercentage = 0;

    @Field("healthvia_partner_discount")
    private Integer healthviaPartnerDiscount = 0; // HealthVia ortaklık indirimi

    // === OLANAKLAR (Amenities) ===
    private Set<String> amenities; // Wifi, Pool, Spa, Gym, Parking, etc.

    @Field("room_amenities")
    private Set<String> roomAmenities; // Minibar, AC, TV, Safe, etc.

    @Field("has_airport_transfer")
    private Boolean hasAirportTransfer = false;

    @Field("has_hospital_transfer")
    private Boolean hasHospitalTransfer = false;

    @Field("has_24_hour_reception")
    private Boolean has24HourReception = true;

    @Field("has_restaurant")
    private Boolean hasRestaurant = false;

    @Field("has_room_service")
    private Boolean hasRoomService = false;

    // === DEĞERLENDİRME & YORUM ===
    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0")
    private Double rating = 0.0;

    @Field("review_count")
    @Min(value = 0)
    private Integer reviewCount = 0;

    @Field("cleanliness_rating")
    private Double cleanlinessRating;

    @Field("service_rating")
    private Double serviceRating;

    @Field("location_rating")
    private Double locationRating;

    @Field("value_rating")
    private Double valueRating;

    // === ODA BİLGİLERİ ===
    @Field("total_rooms")
    private Integer totalRooms;

    @Field("available_rooms")
    private Integer availableRooms;

    @Field("room_types")
    private List<RoomType> roomTypes;

    // === İLETİŞİM BİLGİLERİ ===
    private String phone;
    private String email;
    private String website;

    @Field("whatsapp_number")
    private String whatsappNumber;

    // === GÖRSELLER ===
    @Field("main_image_url")
    private String mainImageUrl;

    @Field("image_urls")
    private List<String> imageUrls;

    @Field("virtual_tour_url")
    private String virtualTourUrl;

    // === POLİTİKALAR ===
    @Field("check_in_time")
    private String checkInTime = "14:00";

    @Field("check_out_time")
    private String checkOutTime = "12:00";

    @Field("cancellation_policy")
    private String cancellationPolicy;

    @Field("pet_policy")
    private String petPolicy;

    @Field("child_policy")
    private String childPolicy;

    // === DURUM ===
    @Indexed
    private HotelStatus status = HotelStatus.ACTIVE;

    @Field("is_featured")
    private Boolean isFeatured = false;

    @Field("is_verified")
    private Boolean isVerified = false;

    @Field("verified_at")
    private LocalDateTime verifiedAt;

    // === ENUMS ===
    public enum HotelStatus {
        ACTIVE,         // Aktif, rezervasyona açık
        INACTIVE,       // Geçici olarak kapalı
        MAINTENANCE,    // Bakımda
        SUSPENDED,      // Askıya alınmış
        PENDING_REVIEW  // İnceleme bekliyor
    }

    // === İÇ SINIFLAR ===
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomType {
        private String name;            // Standard, Deluxe, Suite, Patient Room
        private String description;
        private Integer capacity;       // Kişi kapasitesi
        private BigDecimal pricePerNight;
        private Set<String> amenities;
        private Integer availableCount;
        private List<String> imageUrls;
        private Boolean isMedicalRoom = false;
    }

    // === HELPER METHODS ===
    public BigDecimal getDiscountedPrice() {
        if (discountPercentage == null || discountPercentage == 0) {
            return pricePerNight;
        }
        BigDecimal discount = pricePerNight.multiply(BigDecimal.valueOf(discountPercentage))
            .divide(BigDecimal.valueOf(100));
        return pricePerNight.subtract(discount);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (district != null) sb.append(", ").append(district);
        if (province != null) sb.append(", ").append(province);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }

    public boolean isNearHospital() {
        return distanceToHospitalKm != null && distanceToHospitalKm <= 3.0;
    }

    public boolean hasAmenity(String amenity) {
        return amenities != null && amenities.contains(amenity);
    }
}
