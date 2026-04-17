package com.healthvia.platform.booking.hotel.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.booking.hotel.entity.HotelBooking;
import com.healthvia.platform.booking.hotel.service.HotelBookingService;
import com.healthvia.platform.booking.provider.BookingProvider;
import com.healthvia.platform.common.dto.ApiResponse;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
public class HotelBookingController {

    private final HotelBookingService hotelBookingService;

    @PostMapping("/search")
    public ApiResponse<List<BookingProvider.HotelSearchResult>> search(
            @RequestBody HotelSearchRequest req) {
        var list = hotelBookingService.search(
            req.city(), req.checkIn(), req.checkOut(),
            req.guests() == null ? 1 : req.guests(),
            req.roomType());
        return ApiResponse.success(list);
    }

    @PostMapping("/reserve")
    public ApiResponse<HotelBooking> reserve(@RequestBody ReserveRequest req) {
        HotelBooking b = hotelBookingService.reserveOnBehalf(
            req.caseId(),
            req.hotelId(),
            req.roomType(),
            new BookingProvider.GuestInfo(
                req.guest().firstName(),
                req.guest().lastName(),
                req.guest().passportNumber(),
                req.guest().phone(),
                req.guest().email()),
            req.checkIn(),
            req.checkOut(),
            req.guests() == null ? 1 : req.guests(),
            req.consentId());
        return ApiResponse.success(b, "Otel rezerve edildi");
    }

    @GetMapping("/bookings")
    public ApiResponse<List<HotelBooking>> listByCase(@RequestParam @NotBlank String caseId) {
        return ApiResponse.success(hotelBookingService.findByCase(caseId));
    }

    public record HotelSearchRequest(
        String city,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        Integer guests,
        String roomType) {}

    public record ReserveRequest(
        String caseId,
        String hotelId,
        String roomType,
        GuestPayload guest,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        Integer guests,
        String consentId) {}

    public record GuestPayload(
        String firstName,
        String lastName,
        String passportNumber,
        String phone,
        String email) {}
}
