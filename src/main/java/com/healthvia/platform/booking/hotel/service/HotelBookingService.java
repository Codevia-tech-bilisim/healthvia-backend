package com.healthvia.platform.booking.hotel.service;

import java.time.LocalDate;
import java.util.List;

import com.healthvia.platform.booking.hotel.entity.HotelBooking;
import com.healthvia.platform.booking.provider.BookingProvider;

public interface HotelBookingService {

    List<BookingProvider.HotelSearchResult> search(
        String city, LocalDate checkIn, LocalDate checkOut, int guests, String roomType);

    HotelBooking reserveOnBehalf(
        String caseId,
        String hotelProviderId,
        String roomType,
        BookingProvider.GuestInfo guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        String consentId);

    List<HotelBooking> findByCase(String caseId);
}
