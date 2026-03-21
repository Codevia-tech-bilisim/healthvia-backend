package com.healthvia.platform.booking;

import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.repository.AppointmentRepository;
import com.healthvia.platform.auth.security.UserPrincipal;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final AppointmentRepository appointmentRepository;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Appointment apt = appointmentRepository.findById(req.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", req.getAppointmentId()));

        // Ownership check
        if (!apt.getPatientId().equals(principal.getId())) {
            return ApiResponse.error("This appointment does not belong to you");
        }

        // Save hotel/flight info and prices
        if (req.getHotelId() != null) {
            apt.setHotelBookingId(req.getHotelId());
            apt.setHotelBookingName(req.getHotelName());
            if (req.getHotelPrice() != null) {
                apt.setHotelPrice(req.getHotelPrice());
            }
        }
        if (req.getFlightId() != null) {
            apt.setFlightBookingId(req.getFlightId());
            apt.setFlightBookingDetails(req.getFlightDetails());
            if (req.getFlightPrice() != null) {
                apt.setFlightPrice(req.getFlightPrice());
            }
        }

        // Recalculate total price
        java.math.BigDecimal total = apt.getConsultationFee() != null
            ? apt.getConsultationFee() : java.math.BigDecimal.ZERO;
        if (apt.getHotelPrice() != null) total = total.add(apt.getHotelPrice());
        if (apt.getFlightPrice() != null) total = total.add(apt.getFlightPrice());
        apt.setTotalPrice(total);

        // Fake payment processing
        if ("tok_test_success".equals(req.getPaymentToken())) {
            apt.setPaymentStatus(Appointment.PaymentStatus.PAID);
        } else {
            return ApiResponse.error("Payment failed");
        }

        appointmentRepository.save(apt);

        // Zoom link if available
        String joinUrl = null;
        String startUrl = null;
        if (apt.getMeetingInfo() != null) {
            joinUrl = apt.getMeetingInfo().getMeetingUrl();
        }

        return ApiResponse.success(BookingResponse.builder()
                .bookingId(apt.getId())
                .appointmentId(apt.getId())
                .status("CONFIRMED")
                .zoomJoinUrl(joinUrl)
                .zoomStartUrl(startUrl)
                .hotelName(apt.getHotelBookingName())
                .flightDetails(apt.getFlightBookingDetails())
                .message("Your booking is confirmed!")
                .build(), "Booking created successfully");
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ApiResponse<BookingResponse> getBookingByAppointment(
            @PathVariable String appointmentId) {

        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        String joinUrl = apt.getMeetingInfo() != null ? apt.getMeetingInfo().getMeetingUrl() : null;

        return ApiResponse.success(BookingResponse.builder()
                .bookingId(apt.getId())
                .appointmentId(apt.getId())
                .status(apt.getStatus().name())
                .zoomJoinUrl(joinUrl)
                .hotelName(apt.getHotelBookingName())
                .flightDetails(apt.getFlightBookingDetails())
                .build());
    }
}
