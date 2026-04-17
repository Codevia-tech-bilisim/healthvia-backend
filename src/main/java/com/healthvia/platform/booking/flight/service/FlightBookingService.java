package com.healthvia.platform.booking.flight.service;

import java.time.LocalDate;
import java.util.List;

import com.healthvia.platform.booking.flight.entity.FlightBooking;
import com.healthvia.platform.booking.provider.BookingProvider;

public interface FlightBookingService {

    List<BookingProvider.FlightSearchResult> search(
        String fromCode, String toCode, LocalDate departureDate,
        int passengers, String cabinClass);

    FlightBooking reserveOnBehalf(
        String caseId,
        String flightProviderId,
        BookingProvider.PassengerInfo passenger,
        String consentId);

    List<FlightBooking> findByCase(String caseId);
}
