package com.healthvia.platform.booking.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking transport abstraction for flights + hotels. Implementations:
 *   - MockBookingProvider → reads inventory from Payload CMS, mints mock PNRs.
 *   - AmadeusBookingProvider (stub) → real flight GDS, post-license activation.
 *   - ExpediaHotelProvider (stub) → real hotel booking.
 *
 * Selection via `booking.provider=mock|amadeus` property.
 * A single interface covers both so strategy stays uniform across vendors.
 */
public interface BookingProvider {

    BookingProviderType getType();

    /* ---------- Flights ---------- */
    List<FlightSearchResult> searchFlights(FlightSearchCriteria criteria);

    FlightBookingResult reserveFlight(String flightProviderId, PassengerInfo passenger);

    /* ---------- Hotels ---------- */
    List<HotelSearchResult> searchHotels(HotelSearchCriteria criteria);

    HotelBookingResult reserveHotel(
        String hotelProviderId,
        String roomType,
        GuestInfo guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount);

    enum BookingProviderType { MOCK, AMADEUS, EXPEDIA }

    /* ---------- Value objects ---------- */

    record FlightSearchCriteria(
        String fromCode,
        String toCode,
        LocalDate departureDate,
        int passengers,
        String cabinClass) {}

    record FlightSearchResult(
        String providerId,
        String airline,
        String flightNumber,
        String departureAirport,
        String departureAirportCode,
        String arrivalAirport,
        String arrivalAirportCode,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String duration,
        int stops,
        String cabinClass,
        BigDecimal price,
        String currency,
        int availableSeats) {}

    record FlightBookingResult(
        String providerId,
        String pnr,
        String status) {}

    record HotelSearchCriteria(
        String city,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String roomType) {}

    record HotelSearchResult(
        String providerId,
        String name,
        String city,
        String address,
        int starRating,
        double userRating,
        BigDecimal pricePerNight,
        String currency,
        String nearestHospital,
        Double distanceToHospitalKm) {}

    record HotelBookingResult(
        String providerId,
        String confirmationNumber,
        String status,
        BigDecimal totalPrice,
        int nights) {}

    record PassengerInfo(
        String firstName,
        String lastName,
        String passportNumber,
        String nationality,
        LocalDate dateOfBirth,
        String gender,
        String email,
        String phone) {}

    record GuestInfo(
        String firstName,
        String lastName,
        String passportNumber,
        String phone,
        String email) {}
}
