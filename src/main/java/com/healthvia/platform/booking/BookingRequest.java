package com.healthvia.platform.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRequest {
    @NotBlank
    private String appointmentId;
    private String hotelId;
    private String hotelName;
    private String flightId;
    private String flightDetails;
    @NotBlank
    private String paymentToken;
}
