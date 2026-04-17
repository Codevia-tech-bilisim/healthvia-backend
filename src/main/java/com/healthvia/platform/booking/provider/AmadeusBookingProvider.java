package com.healthvia.platform.booking.provider;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Post-license Amadeus GDS adapter. Activate by setting
 *   booking.provider=amadeus
 *   amadeus.client-id=…
 *   amadeus.client-secret=…
 *
 * Right now this is a scaffold — it compiles, keeps the interface contract,
 * and intentionally returns an empty list / throws on reserve. When the
 * license is signed, swap the stubs with real Amadeus REST calls.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "booking.provider", havingValue = "amadeus")
public class AmadeusBookingProvider implements BookingProvider {

    @Override
    public BookingProviderType getType() {
        return BookingProviderType.AMADEUS;
    }

    @Override
    public List<FlightSearchResult> searchFlights(FlightSearchCriteria criteria) {
        log.warn("[AMADEUS] searchFlights not yet implemented — returning empty list");
        return List.of();
    }

    @Override
    public FlightBookingResult reserveFlight(String flightProviderId, PassengerInfo passenger) {
        throw new UnsupportedOperationException("Amadeus reservation not yet implemented");
    }

    @Override
    public List<HotelSearchResult> searchHotels(HotelSearchCriteria criteria) {
        return List.of();
    }

    @Override
    public HotelBookingResult reserveHotel(
            String hotelProviderId, String roomType, GuestInfo guest,
            LocalDate checkIn, LocalDate checkOut, int guestCount) {
        throw new UnsupportedOperationException("Amadeus hotel reservation not yet implemented");
    }
}
