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
    @NotBlank
    private String paymentToken;
}
