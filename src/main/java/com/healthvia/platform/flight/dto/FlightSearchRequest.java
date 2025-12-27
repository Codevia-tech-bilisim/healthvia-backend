// flight/dto/FlightSearchRequest.java
package com.healthvia.platform.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.healthvia.platform.flight.entity.Flight.CabinClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {

    // === ROTA BİLGİLERİ ===
    private String departureAirportCode;  // IST, SAW, ESB
    private String arrivalAirportCode;
    private String departureCity;
    private String arrivalCity;
    private String departureCountry;
    private String arrivalCountry;
    
    // === TARİH FİLTRELERİ ===
    private LocalDate departureDate;
    private LocalDate returnDate;           // Gidiş-dönüş için
    private LocalDate flexibleDateStart;    // Esnek tarih başlangıcı
    private LocalDate flexibleDateEnd;      // Esnek tarih bitişi
    private Boolean isRoundTrip = false;
    
    // === SAAT FİLTRELERİ ===
    private LocalTime minDepartureTime;
    private LocalTime maxDepartureTime;
    private LocalTime minArrivalTime;
    private LocalTime maxArrivalTime;
    
    // === YOLCU BİLGİLERİ ===
    private Integer passengers;
    private Integer adults;
    private Integer children;
    private Integer infants;
    
    // === FİYAT FİLTRELERİ ===
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    
    // === KABİN & KOLTUK ===
    private CabinClass cabinClass;
    private Integer minAvailableSeats;
    
    // === UÇUŞ TİPİ ===
    private Boolean directOnly;           // Sadece direkt uçuşlar
    private Integer maxStops;             // Maksimum aktarma sayısı
    
    // === HAVAYOLU FİLTRELERİ ===
    private String airline;
    private String airlineCode;
    private List<String> preferredAirlines;
    private List<String> excludedAirlines;
    
    // === SÜRE FİLTRELERİ ===
    private Integer maxDurationMinutes;   // Maksimum uçuş süresi
    private Integer minDurationMinutes;
    
    // === SAĞLIK TURİZMİ FİLTRELERİ ===
    private Boolean isMedicalTravelFriendly;
    private Boolean wheelchairAssistance;
    private Boolean stretcherAvailable;
    private Boolean medicalEquipmentAllowed;
    
    // === HİZMET FİLTRELERİ ===
    private Boolean wifiAvailable;
    private Boolean mealIncluded;
    private Boolean inFlightEntertainment;
    private List<String> specialMealOptions;  // Medical, Diabetic, etc.
    
    // === BAGAJ FİLTRELERİ ===
    private Boolean checkedBaggageIncluded;
    private Integer minCheckedBags;
    
    // === REZERVASYON FİLTRELERİ ===
    private Boolean refundable;
    private Boolean changeable;
    
    // === PARTNER FİLTRELERİ ===
    private Boolean isHealthviaPartner;
    private Boolean hasPartnerDiscount;
    
    // === SIRALAMA ===
    private SortBy sortBy;
    private SortDirection sortDirection;
    
    // === SAYFALAMA ===
    private Integer page;
    private Integer size;

    // === ENUMS ===
    public enum SortBy {
        PRICE,              // Fiyata göre
        DEPARTURE_TIME,     // Kalkış saatine göre
        ARRIVAL_TIME,       // Varış saatine göre
        DURATION,           // Süreye göre
        AIRLINE,            // Havayoluna göre
        STOPS               // Aktarma sayısına göre
    }
    
    public enum SortDirection {
        ASC,
        DESC
    }

    // === HELPER METHODS ===
    
    /**
     * Varsayılan değerleri uygula
     */
    public FlightSearchRequest withDefaults() {
        if (this.page == null) this.page = 0;
        if (this.size == null) this.size = 20;
        if (this.sortBy == null) this.sortBy = SortBy.PRICE;
        if (this.sortDirection == null) this.sortDirection = SortDirection.ASC;
        if (this.passengers == null) this.passengers = 1;
        if (this.adults == null) this.adults = 1;
        if (this.cabinClass == null) this.cabinClass = CabinClass.ECONOMY;
        if (this.currency == null) this.currency = "USD";
        return this;
    }
    
    /**
     * Geçerli tarih kontrolü
     */
    public boolean hasValidDates() {
        if (departureDate == null) {
            return false;
        }
        if (departureDate.isBefore(LocalDate.now())) {
            return false;
        }
        if (isRoundTrip && returnDate != null && returnDate.isBefore(departureDate)) {
            return false;
        }
        return true;
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
     * Rota bilgisi tam mı?
     */
    public boolean hasCompleteRoute() {
        return (departureAirportCode != null || departureCity != null) &&
               (arrivalAirportCode != null || arrivalCity != null);
    }
    
    /**
     * Sağlık turizmi filtresi uygulanmış mı?
     */
    public boolean hasMedicalFilters() {
        return isMedicalTravelFriendly != null ||
               wheelchairAssistance != null ||
               stretcherAvailable != null ||
               medicalEquipmentAllowed != null;
    }
    
    /**
     * Esnek tarih araması mı?
     */
    public boolean isFlexibleDateSearch() {
        return flexibleDateStart != null && flexibleDateEnd != null;
    }
    
    /**
     * Toplam yolcu sayısı
     */
    public int getTotalPassengers() {
        int total = 0;
        if (adults != null) total += adults;
        if (children != null) total += children;
        if (infants != null) total += infants;
        return total > 0 ? total : (passengers != null ? passengers : 1);
    }
    
    /**
     * Uçuş rota özeti
     */
    public String getRouteDescription() {
        String from = departureAirportCode != null ? departureAirportCode : departureCity;
        String to = arrivalAirportCode != null ? arrivalAirportCode : arrivalCity;
        return from + " → " + to;
    }
}
