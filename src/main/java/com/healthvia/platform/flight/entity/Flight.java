// flight/entity/Flight.java
package com.healthvia.platform.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
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
@Document(collection = "flights")
@CompoundIndex(def = "{'departureAirportCode': 1, 'arrivalAirportCode': 1, 'departureDate': 1}")
@CompoundIndex(def = "{'airline': 1, 'flightNumber': 1, 'departureDate': 1}")
public class Flight extends BaseEntity {

    // === UÇUŞ TEMEL BİLGİLERİ ===
    @NotBlank(message = "Havayolu şirketi zorunludur")
    @Indexed
    private String airline;

    @Field("airline_code")
    private String airlineCode; // TK, PC, BA, etc.

    @Field("airline_logo_url")
    private String airlineLogoUrl;

    @NotBlank(message = "Uçuş numarası zorunludur")
    @Field("flight_number")
    @Indexed
    private String flightNumber; // TK 1984

    @Field("aircraft_type")
    private String aircraftType; // Boeing 737, Airbus A320, etc.

    // === KALKIŞ BİLGİLERİ ===
    @NotBlank(message = "Kalkış havalimanı kodu zorunludur")
    @Field("departure_airport_code")
    @Indexed
    private String departureAirportCode; // IST, SAW, ESB

    @Field("departure_airport_name")
    private String departureAirportName;

    @NotBlank(message = "Kalkış şehri zorunludur")
    @Field("departure_city")
    private String departureCity;

    @Field("departure_country")
    private String departureCountry;

    @Field("departure_terminal")
    private String departureTerminal;

    @NotNull(message = "Kalkış tarihi zorunludur")
    @Field("departure_date")
    @Indexed
    private LocalDate departureDate;

    @NotNull(message = "Kalkış saati zorunludur")
    @Field("departure_time")
    private LocalTime departureTime;

    @Field("departure_datetime")
    private LocalDateTime departureDatetime;

    // === VARIŞ BİLGİLERİ ===
    @NotBlank(message = "Varış havalimanı kodu zorunludur")
    @Field("arrival_airport_code")
    @Indexed
    private String arrivalAirportCode;

    @Field("arrival_airport_name")
    private String arrivalAirportName;

    @NotBlank(message = "Varış şehri zorunludur")
    @Field("arrival_city")
    private String arrivalCity;

    @Field("arrival_country")
    private String arrivalCountry;

    @Field("arrival_terminal")
    private String arrivalTerminal;

    @NotNull(message = "Varış tarihi zorunludur")
    @Field("arrival_date")
    private LocalDate arrivalDate;

    @NotNull(message = "Varış saati zorunludur")
    @Field("arrival_time")
    private LocalTime arrivalTime;

    @Field("arrival_datetime")
    private LocalDateTime arrivalDatetime;

    // === UÇUŞ SÜRESİ ===
    @Field("duration_minutes")
    private Integer durationMinutes;

    @Field("duration_formatted")
    private String durationFormatted; // "4h 15m"

    // === AKTARMA BİLGİLERİ ===
    @Field("is_direct")
    private Boolean isDirect = true;

    @Field("stops_count")
    private Integer stopsCount = 0;

    @Field("stops")
    private List<FlightStop> stops;

    // === FİYATLANDIRMA ===
    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    @Field("base_price")
    private BigDecimal basePrice;

    @Field("currency")
    private String currency = "USD";

    @Field("taxes_and_fees")
    private BigDecimal taxesAndFees;

    @Field("total_price")
    private BigDecimal totalPrice;

    @Field("price_per_adult")
    private BigDecimal pricePerAdult;

    @Field("price_per_child")
    private BigDecimal pricePerChild;

    @Field("price_per_infant")
    private BigDecimal pricePerInfant;

    // === KOLTUK SINIFI ===
    @Field("cabin_class")
    private CabinClass cabinClass = CabinClass.ECONOMY;

    @Field("available_seats")
    private Integer availableSeats;

    @Field("seat_pitch")
    private String seatPitch; // Koltuk aralığı (31", 32", etc.)

    @Field("seat_width")
    private String seatWidth;

    // === BAGAJ BİLGİLERİ ===
    @Field("baggage_allowance")
    private BaggageAllowance baggageAllowance;

    // === SAĞLIK TURİZMİ ÖZELLİKLERİ ===
    @Field("is_medical_travel_friendly")
    private Boolean isMedicalTravelFriendly = false;

    @Field("wheelchair_assistance")
    private Boolean wheelchairAssistance = false;

    @Field("stretcher_available")
    private Boolean stretcherAvailable = false;

    @Field("medical_equipment_allowed")
    private Boolean medicalEquipmentAllowed = false;

    @Field("special_meal_options")
    private Set<String> specialMealOptions; // Medical, Low-sodium, Diabetic, etc.

    // === HİZMETLER ===
    @Field("in_flight_entertainment")
    private Boolean inFlightEntertainment = false;

    @Field("wifi_available")
    private Boolean wifiAvailable = false;

    @Field("usb_power")
    private Boolean usbPower = false;

    @Field("meal_included")
    private Boolean mealIncluded = false;

    @Field("meal_types")
    private Set<String> mealTypes; // Breakfast, Lunch, Dinner, Snack

    // === REZERVASYON BİLGİLERİ ===
    @Field("booking_class")
    private String bookingClass; // Y, B, M, etc.

    @Field("fare_basis")
    private String fareBasis;

    @Field("refundable")
    private Boolean refundable = false;

    @Field("changeable")
    private Boolean changeable = false;

    @Field("change_fee")
    private BigDecimal changeFee;

    // === DURUM ===
    @Indexed
    private FlightStatus status = FlightStatus.SCHEDULED;

    @Field("is_healthvia_partner")
    private Boolean isHealthviaPartner = false;

    @Field("partner_discount_percentage")
    private Integer partnerDiscountPercentage = 0;

    // === ENUMS ===
    public enum CabinClass {
        ECONOMY,
        PREMIUM_ECONOMY,
        BUSINESS,
        FIRST
    }

    public enum FlightStatus {
        SCHEDULED,      // Planlandı
        ON_TIME,        // Zamanında
        DELAYED,        // Gecikti
        BOARDING,       // Biniş yapılıyor
        DEPARTED,       // Kalktı
        IN_AIR,         // Havada
        LANDED,         // İndi
        ARRIVED,        // Vardı
        CANCELLED,      // İptal
        DIVERTED        // Yönlendirildi
    }

    // === İÇ SINIFLAR ===
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightStop {
        private String airportCode;
        private String airportName;
        private String city;
        private LocalDateTime arrivalTime;
        private LocalDateTime departureTime;
        private Integer durationMinutes; // Aktarma süresi
        private Boolean changeAircraft = false;
        private String flightNumber; // Aktarma uçuş numarası
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaggageAllowance {
        private Integer carryOnBags;        // Kabin bagajı sayısı
        private String carryOnWeight;       // "7 kg"
        private String carryOnDimensions;   // "55x40x23 cm"
        
        private Integer checkedBags;        // Kayıtlı bagaj sayısı
        private String checkedWeight;       // "23 kg"
        
        private BigDecimal extraBagFee;     // Ek bagaj ücreti
        
        private Boolean personalItemAllowed = true;
    }

    // === HELPER METHODS ===
    public String getRoute() {
        return departureAirportCode + " → " + arrivalAirportCode;
    }

    public String getFullRoute() {
        return departureCity + " (" + departureAirportCode + ") → " + 
               arrivalCity + " (" + arrivalAirportCode + ")";
    }

    public BigDecimal getCalculatedTotalPrice() {
        if (totalPrice != null) return totalPrice;
        if (basePrice == null) return BigDecimal.ZERO;
        return basePrice.add(taxesAndFees != null ? taxesAndFees : BigDecimal.ZERO);
    }

    public String getFormattedDuration() {
        if (durationFormatted != null) return durationFormatted;
        if (durationMinutes == null) return "";
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    public boolean isOvernight() {
        return !departureDate.equals(arrivalDate);
    }

    public boolean isDomestic() {
        return departureCountry != null && departureCountry.equals(arrivalCountry);
    }

    // Kalkış-varış zamanlarını hesapla
    public void calculateDatetimes() {
        if (departureDate != null && departureTime != null) {
            this.departureDatetime = LocalDateTime.of(departureDate, departureTime);
        }
        if (arrivalDate != null && arrivalTime != null) {
            this.arrivalDatetime = LocalDateTime.of(arrivalDate, arrivalTime);
        }
        if (departureDatetime != null && arrivalDatetime != null) {
            this.durationMinutes = (int) Duration.between(departureDatetime, arrivalDatetime).toMinutes();
        }
    }
}
