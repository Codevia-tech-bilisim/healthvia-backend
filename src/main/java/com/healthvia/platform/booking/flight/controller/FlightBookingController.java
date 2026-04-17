package com.healthvia.platform.booking.flight.controller;

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

import com.healthvia.platform.booking.flight.entity.FlightBooking;
import com.healthvia.platform.booking.flight.service.FlightBookingService;
import com.healthvia.platform.booking.provider.BookingProvider;
import com.healthvia.platform.common.dto.ApiResponse;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
public class FlightBookingController {

    private final FlightBookingService flightBookingService;

    @PostMapping("/search")
    public ApiResponse<List<BookingProvider.FlightSearchResult>> search(
            @RequestBody FlightSearchRequest req) {
        var list = flightBookingService.search(
            req.fromCode(), req.toCode(), req.departureDate(),
            req.passengers() == null ? 1 : req.passengers(),
            req.cabinClass());
        return ApiResponse.success(list);
    }

    @PostMapping("/reserve")
    public ApiResponse<FlightBooking> reserve(@RequestBody ReserveRequest req) {
        FlightBooking booking = flightBookingService.reserveOnBehalf(
            req.caseId(),
            req.flightId(),
            new BookingProvider.PassengerInfo(
                req.passenger().firstName(),
                req.passenger().lastName(),
                req.passenger().passportNumber(),
                req.passenger().nationality(),
                req.passenger().dateOfBirth(),
                req.passenger().gender(),
                req.passenger().email(),
                req.passenger().phone()),
            req.consentId());
        return ApiResponse.success(booking, "Uçuş rezerve edildi");
    }

    @GetMapping("/bookings")
    public ApiResponse<List<FlightBooking>> listByCase(@RequestParam @NotBlank String caseId) {
        return ApiResponse.success(flightBookingService.findByCase(caseId));
    }

    public record FlightSearchRequest(
        String fromCode,
        String toCode,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
        Integer passengers,
        String cabinClass) {}

    public record ReserveRequest(
        String caseId,
        String flightId,
        PassengerPayload passenger,
        String consentId) {}

    public record PassengerPayload(
        String firstName,
        String lastName,
        String passportNumber,
        String nationality,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
        String gender,
        String email,
        String phone) {}
}
