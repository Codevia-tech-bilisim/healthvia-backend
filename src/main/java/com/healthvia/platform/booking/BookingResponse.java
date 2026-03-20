package com.healthvia.platform.booking;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {
    private String bookingId;
    private String appointmentId;
    private String status;
    private String zoomJoinUrl;
    private String zoomStartUrl;
    private String hotelName;
    private String flightDetails;
    private String message;
}
