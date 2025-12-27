// hotel/dto/HotelCreateRequest.java
package com.healthvia.platform.hotel.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.hotel.entity.Hotel;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelCreateRequest {

    @NotBlank(message = "Otel adı zorunludur")
    @Size(min = 2, max = 200, message = "Otel adı 2-200 karakter arasında olmalıdır")
    private String name;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    @Min(value = 1, message = "Yıldız en az 1 olmalıdır")
    @Max(value = 5, message = "Yıldız en fazla 5 olabilir")
    private Integer starRating;

    // === KONUM ===
    @NotBlank(message = "İl zorunludur")
    private String province;

    private String district;

    @NotBlank(message = "Adres zorunludur")
    private String address;

    private String postalCode;
    private String country = "Turkey";
    private Double latitude;
    private Double longitude;

    // === SAĞLIK TURİZMİ ===
    private Boolean isMedicalFocus = false;
    private Double distanceToHospitalKm;
    private List<String> partnerHospitals;
    private Set<String> medicalServices;
    private Boolean hasMedicalStaff = false;
    private Boolean wheelchairAccessible = false;
    private Boolean hasPatientRooms = false;

    // === FİYATLANDIRMA ===
    @NotNull(message = "Gecelik fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal pricePerNight;

    private String currency = "USD";
    private Boolean priceIncludesBreakfast = false;
    private Integer discountPercentage = 0;
    private Integer healthviaPartnerDiscount = 0;

    // === OLANAKLAR ===
    private Set<String> amenities;
    private Set<String> roomAmenities;
    private Boolean hasAirportTransfer = false;
    private Boolean hasHospitalTransfer = false;
    private Boolean has24HourReception = true;
    private Boolean hasRestaurant = false;
    private Boolean hasRoomService = false;

    // === ODA BİLGİLERİ ===
    private Integer totalRooms;
    private Integer availableRooms;
    private List<Hotel.RoomType> roomTypes;

    // === İLETİŞİM ===
    private String phone;

    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    private String website;
    private String whatsappNumber;

    // === GÖRSELLER ===
    private String mainImageUrl;
    private List<String> imageUrls;
    private String virtualTourUrl;

    // === POLİTİKALAR ===
    private String checkInTime = "14:00";
    private String checkOutTime = "12:00";
    private String cancellationPolicy;
    private String petPolicy;
    private String childPolicy;

    // === Entity'ye Dönüştürme ===
    public Hotel toEntity() {
        return Hotel.builder()
            .name(name)
            .description(description)
            .starRating(starRating)
            // Konum
            .province(province)
            .district(district)
            .address(address)
            .postalCode(postalCode)
            .country(country)
            .latitude(latitude)
            .longitude(longitude)
            // Sağlık Turizmi
            .isMedicalFocus(isMedicalFocus)
            .distanceToHospitalKm(distanceToHospitalKm)
            .partnerHospitals(partnerHospitals)
            .medicalServices(medicalServices)
            .hasMedicalStaff(hasMedicalStaff)
            .wheelchairAccessible(wheelchairAccessible)
            .hasPatientRooms(hasPatientRooms)
            // Fiyatlandırma
            .pricePerNight(pricePerNight)
            .currency(currency)
            .priceIncludesBreakfast(priceIncludesBreakfast)
            .discountPercentage(discountPercentage)
            .healthviaPartnerDiscount(healthviaPartnerDiscount)
            // Olanaklar
            .amenities(amenities)
            .roomAmenities(roomAmenities)
            .hasAirportTransfer(hasAirportTransfer)
            .hasHospitalTransfer(hasHospitalTransfer)
            .has24HourReception(has24HourReception)
            .hasRestaurant(hasRestaurant)
            .hasRoomService(hasRoomService)
            // Oda Bilgileri
            .totalRooms(totalRooms)
            .availableRooms(availableRooms)
            .roomTypes(roomTypes)
            // İletişim
            .phone(phone)
            .email(email)
            .website(website)
            .whatsappNumber(whatsappNumber)
            // Görseller
            .mainImageUrl(mainImageUrl)
            .imageUrls(imageUrls)
            .virtualTourUrl(virtualTourUrl)
            // Politikalar
            .checkInTime(checkInTime)
            .checkOutTime(checkOutTime)
            .cancellationPolicy(cancellationPolicy)
            .petPolicy(petPolicy)
            .childPolicy(childPolicy)
            // Varsayılan değerler
            .rating(0.0)
            .reviewCount(0)
            .build();
    }
}
