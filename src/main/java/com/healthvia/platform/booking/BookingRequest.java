package com.healthvia.platform.booking;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRequest {
    @NotBlank
    private String appointmentId;
    private String hotelId;
    private String hotelName;
    private BigDecimal hotelPrice;
    private String flightId;
    private String flightDetails;
    private BigDecimal flightPrice;

    // iyzico card details
    @NotBlank
    private String cardNumber;
    @NotBlank
    private String expireMonth;
    @NotBlank
    private String expireYear;
    @NotBlank
    private String cvc;
    @NotBlank
    private String cardHolderName;
}
