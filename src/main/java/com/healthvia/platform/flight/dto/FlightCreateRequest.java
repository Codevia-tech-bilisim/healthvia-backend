// flight/dto/FlightCreateRequest.java
package com.healthvia.platform.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.flight.entity.Flight;
import com.healthvia.platform.flight.entity.Flight.CabinClass;
import com.healthvia.platform.flight.entity.Flight.BaggageAllowance;
import com.healthvia.platform.flight.entity.Flight.FlightStop;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightCreateRequest {

    // Havayolu Bilgileri
    @NotBlank(message = "Havayolu şirketi zorunludur")
    private String airline;
    
    private String airlineCode;
    private String airlineLogoUrl;
    
    @NotBlank(message = "Uçuş numarası zorunludur")
    private String flightNumber;
    
    private String aircraftType;

    // Kalkış Bilgileri
    @NotBlank(message = "Kalkış havalimanı kodu zorunludur")
    private String departureAirportCode;
    
    private String departureAirportName;
    
    @NotBlank(message = "Kalkış şehri zorunludur")
    private String departureCity;
    
    private String departureCountry;
    private String departureTerminal;
    
    @NotNull(message = "Kalkış tarihi zorunludur")
    private LocalDate departureDate;
    
    @NotNull(message = "Kalkış saati zorunludur")
    private LocalTime departureTime;

    // Varış Bilgileri
    @NotBlank(message = "Varış havalimanı kodu zorunludur")
    private String arrivalAirportCode;
    
    private String arrivalAirportName;
    
    @NotBlank(message = "Varış şehri zorunludur")
    private String arrivalCity;
    
    private String arrivalCountry;
    private String arrivalTerminal;
    
    @NotNull(message = "Varış tarihi zorunludur")
    private LocalDate arrivalDate;
    
    @NotNull(message = "Varış saati zorunludur")
    private LocalTime arrivalTime;

    // Aktarma Bilgileri
    private Boolean isDirect = true;
    private Integer stopsCount = 0;
    private List<FlightStop> stops;

    // Fiyatlandırma
    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal basePrice;
    
    private String currency = "USD";
    private BigDecimal taxesAndFees;
    private BigDecimal totalPrice;
    private BigDecimal pricePerAdult;
    private BigDecimal pricePerChild;
    private BigDecimal pricePerInfant;

    // Kabin Sınıfı
    private CabinClass cabinClass = CabinClass.ECONOMY;
    private Integer availableSeats;
    private String seatPitch;
    private String seatWidth;

    // Bagaj
    private BaggageAllowance baggageAllowance;

    // Sağlık Turizmi
    private Boolean isMedicalTravelFriendly = false;
    private Boolean wheelchairAssistance = false;
    private Boolean stretcherAvailable = false;
    private Boolean medicalEquipmentAllowed = false;
    private Set<String> specialMealOptions;

    // Hizmetler
    private Boolean inFlightEntertainment = false;
    private Boolean wifiAvailable = false;
    private Boolean usbPower = false;
    private Boolean mealIncluded = false;
    private Set<String> mealTypes;

    // Rezervasyon
    private String bookingClass;
    private String fareBasis;
    private Boolean refundable = false;
    private Boolean changeable = false;
    private BigDecimal changeFee;

    // Partner
    private Boolean isHealthviaPartner = false;
    private Integer partnerDiscountPercentage = 0;

    // === Factory Method ===
    
    public Flight toEntity() {
        Flight flight = Flight.builder()
            // Havayolu
            .airline(airline)
            .airlineCode(airlineCode)
            .airlineLogoUrl(airlineLogoUrl)
            .flightNumber(flightNumber)
            .aircraftType(aircraftType)
            // Kalkış
            .departureAirportCode(departureAirportCode)
            .departureAirportName(departureAirportName)
            .departureCity(departureCity)
            .departureCountry(departureCountry)
            .departureTerminal(departureTerminal)
            .departureDate(departureDate)
            .departureTime(departureTime)
            // Varış
            .arrivalAirportCode(arrivalAirportCode)
            .arrivalAirportName(arrivalAirportName)
            .arrivalCity(arrivalCity)
            .arrivalCountry(arrivalCountry)
            .arrivalTerminal(arrivalTerminal)
            .arrivalDate(arrivalDate)
            .arrivalTime(arrivalTime)
            // Aktarma
            .isDirect(isDirect)
            .stopsCount(stopsCount)
            .stops(stops)
            // Fiyat
            .basePrice(basePrice)
            .currency(currency)
            .taxesAndFees(taxesAndFees)
            .totalPrice(totalPrice)
            .pricePerAdult(pricePerAdult)
            .pricePerChild(pricePerChild)
            .pricePerInfant(pricePerInfant)
            // Kabin
            .cabinClass(cabinClass)
            .availableSeats(availableSeats)
            .seatPitch(seatPitch)
            .seatWidth(seatWidth)
            // Bagaj
            .baggageAllowance(baggageAllowance)
            // Sağlık Turizmi
            .isMedicalTravelFriendly(isMedicalTravelFriendly)
            .wheelchairAssistance(wheelchairAssistance)
            .stretcherAvailable(stretcherAvailable)
            .medicalEquipmentAllowed(medicalEquipmentAllowed)
            .specialMealOptions(specialMealOptions)
            // Hizmetler
            .inFlightEntertainment(inFlightEntertainment)
            .wifiAvailable(wifiAvailable)
            .usbPower(usbPower)
            .mealIncluded(mealIncluded)
            .mealTypes(mealTypes)
            // Rezervasyon
            .bookingClass(bookingClass)
            .fareBasis(fareBasis)
            .refundable(refundable)
            .changeable(changeable)
            .changeFee(changeFee)
            // Partner
            .isHealthviaPartner(isHealthviaPartner)
            .partnerDiscountPercentage(partnerDiscountPercentage)
            .build();
        
        // Toplam fiyatı hesapla
        if (totalPrice == null && basePrice != null) {
            BigDecimal taxes = taxesAndFees != null ? taxesAndFees : BigDecimal.ZERO;
            flight.setTotalPrice(basePrice.add(taxes));
        }
        
        return flight;
    }
}
