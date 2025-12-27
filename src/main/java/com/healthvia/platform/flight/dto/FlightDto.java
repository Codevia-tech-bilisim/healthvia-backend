// flight/dto/FlightDto.java
package com.healthvia.platform.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.flight.entity.Flight;
import com.healthvia.platform.flight.entity.Flight.CabinClass;
import com.healthvia.platform.flight.entity.Flight.FlightStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightDto {

    private String id;
    
    // Havayolu Bilgileri
    private String airline;
    private String airlineCode;
    private String airlineLogoUrl;
    private String flightNumber;
    private String aircraftType;
    
    // Kalkış Bilgileri
    private String departureAirportCode;
    private String departureAirportName;
    private String departureCity;
    private String departureCountry;
    private String departureTerminal;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalDateTime departureDatetime;
    
    // Varış Bilgileri
    private String arrivalAirportCode;
    private String arrivalAirportName;
    private String arrivalCity;
    private String arrivalCountry;
    private String arrivalTerminal;
    private LocalDate arrivalDate;
    private LocalTime arrivalTime;
    private LocalDateTime arrivalDatetime;
    
    // Süre
    private Integer durationMinutes;
    private String durationFormatted;
    
    // Rota bilgisi (helper)
    private String route;
    private String fullRoute;
    
    // Aktarma Bilgileri
    private Boolean isDirect;
    private Integer stopsCount;
    private List<FlightStopDto> stops;
    
    // Fiyatlandırma
    private BigDecimal basePrice;
    private BigDecimal taxesAndFees;
    private BigDecimal totalPrice;
    private String currency;
    private BigDecimal pricePerAdult;
    private BigDecimal pricePerChild;
    private BigDecimal pricePerInfant;
    
    // Kabin Sınıfı
    private CabinClass cabinClass;
    private Integer availableSeats;
    private String seatPitch;
    private String seatWidth;
    
    // Bagaj
    private BaggageAllowanceDto baggageAllowance;
    
    // Sağlık Turizmi
    private Boolean isMedicalTravelFriendly;
    private Boolean wheelchairAssistance;
    private Boolean stretcherAvailable;
    private Boolean medicalEquipmentAllowed;
    private Set<String> specialMealOptions;
    
    // Hizmetler
    private Boolean inFlightEntertainment;
    private Boolean wifiAvailable;
    private Boolean usbPower;
    private Boolean mealIncluded;
    private Set<String> mealTypes;
    
    // Rezervasyon
    private String bookingClass;
    private String fareBasis;
    private Boolean refundable;
    private Boolean changeable;
    private BigDecimal changeFee;
    
    // Durum
    private FlightStatus status;
    private Boolean isHealthviaPartner;
    private Integer partnerDiscountPercentage;
    
    // Meta
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === Inner DTOs ===
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightStopDto {
        private String airportCode;
        private String airportName;
        private String city;
        private LocalDateTime arrivalTime;
        private LocalDateTime departureTime;
        private Integer durationMinutes;
        private Boolean changeAircraft;
        private String flightNumber;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaggageAllowanceDto {
        private Integer carryOnBags;
        private String carryOnWeight;
        private String carryOnDimensions;
        private Integer checkedBags;
        private String checkedWeight;
        private BigDecimal extraBagFee;
        private Boolean personalItemAllowed;
    }

    // === Factory Method ===
    
    public static FlightDto fromEntity(Flight flight) {
        if (flight == null) return null;
        
        FlightDtoBuilder builder = FlightDto.builder()
            .id(flight.getId())
            // Havayolu
            .airline(flight.getAirline())
            .airlineCode(flight.getAirlineCode())
            .airlineLogoUrl(flight.getAirlineLogoUrl())
            .flightNumber(flight.getFlightNumber())
            .aircraftType(flight.getAircraftType())
            // Kalkış
            .departureAirportCode(flight.getDepartureAirportCode())
            .departureAirportName(flight.getDepartureAirportName())
            .departureCity(flight.getDepartureCity())
            .departureCountry(flight.getDepartureCountry())
            .departureTerminal(flight.getDepartureTerminal())
            .departureDate(flight.getDepartureDate())
            .departureTime(flight.getDepartureTime())
            .departureDatetime(flight.getDepartureDatetime())
            // Varış
            .arrivalAirportCode(flight.getArrivalAirportCode())
            .arrivalAirportName(flight.getArrivalAirportName())
            .arrivalCity(flight.getArrivalCity())
            .arrivalCountry(flight.getArrivalCountry())
            .arrivalTerminal(flight.getArrivalTerminal())
            .arrivalDate(flight.getArrivalDate())
            .arrivalTime(flight.getArrivalTime())
            .arrivalDatetime(flight.getArrivalDatetime())
            // Süre
            .durationMinutes(flight.getDurationMinutes())
            .durationFormatted(flight.getFormattedDuration())
            .route(flight.getRoute())
            .fullRoute(flight.getFullRoute())
            // Aktarma
            .isDirect(flight.getIsDirect())
            .stopsCount(flight.getStopsCount())
            // Fiyat
            .basePrice(flight.getBasePrice())
            .taxesAndFees(flight.getTaxesAndFees())
            .totalPrice(flight.getTotalPrice())
            .currency(flight.getCurrency())
            .pricePerAdult(flight.getPricePerAdult())
            .pricePerChild(flight.getPricePerChild())
            .pricePerInfant(flight.getPricePerInfant())
            // Kabin
            .cabinClass(flight.getCabinClass())
            .availableSeats(flight.getAvailableSeats())
            .seatPitch(flight.getSeatPitch())
            .seatWidth(flight.getSeatWidth())
            // Sağlık Turizmi
            .isMedicalTravelFriendly(flight.getIsMedicalTravelFriendly())
            .wheelchairAssistance(flight.getWheelchairAssistance())
            .stretcherAvailable(flight.getStretcherAvailable())
            .medicalEquipmentAllowed(flight.getMedicalEquipmentAllowed())
            .specialMealOptions(flight.getSpecialMealOptions())
            // Hizmetler
            .inFlightEntertainment(flight.getInFlightEntertainment())
            .wifiAvailable(flight.getWifiAvailable())
            .usbPower(flight.getUsbPower())
            .mealIncluded(flight.getMealIncluded())
            .mealTypes(flight.getMealTypes())
            // Rezervasyon
            .bookingClass(flight.getBookingClass())
            .fareBasis(flight.getFareBasis())
            .refundable(flight.getRefundable())
            .changeable(flight.getChangeable())
            .changeFee(flight.getChangeFee())
            // Durum
            .status(flight.getStatus())
            .isHealthviaPartner(flight.getIsHealthviaPartner())
            .partnerDiscountPercentage(flight.getPartnerDiscountPercentage())
            // Meta
            .createdAt(flight.getCreatedAt())
            .updatedAt(flight.getUpdatedAt());
        
        // Aktarmaları dönüştür
        if (flight.getStops() != null) {
            List<FlightStopDto> stopDtos = flight.getStops().stream()
                .map(stop -> FlightStopDto.builder()
                    .airportCode(stop.getAirportCode())
                    .airportName(stop.getAirportName())
                    .city(stop.getCity())
                    .arrivalTime(stop.getArrivalTime())
                    .departureTime(stop.getDepartureTime())
                    .durationMinutes(stop.getDurationMinutes())
                    .changeAircraft(stop.getChangeAircraft())
                    .flightNumber(stop.getFlightNumber())
                    .build())
                .toList();
            builder.stops(stopDtos);
        }
        
        // Bagaj bilgisini dönüştür
        if (flight.getBaggageAllowance() != null) {
            Flight.BaggageAllowance baggage = flight.getBaggageAllowance();
            builder.baggageAllowance(BaggageAllowanceDto.builder()
                .carryOnBags(baggage.getCarryOnBags())
                .carryOnWeight(baggage.getCarryOnWeight())
                .carryOnDimensions(baggage.getCarryOnDimensions())
                .checkedBags(baggage.getCheckedBags())
                .checkedWeight(baggage.getCheckedWeight())
                .extraBagFee(baggage.getExtraBagFee())
                .personalItemAllowed(baggage.getPersonalItemAllowed())
                .build());
        }
        
        return builder.build();
    }
}
