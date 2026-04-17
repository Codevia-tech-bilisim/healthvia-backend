package com.healthvia.platform.booking.hotel.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.booking.hotel.entity.HotelBooking;
import com.healthvia.platform.booking.hotel.repository.HotelBookingRepository;
import com.healthvia.platform.booking.hotel.service.HotelBookingService;
import com.healthvia.platform.booking.provider.BookingProvider;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.consent.service.ConsentService;
import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.service.PatientCaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HotelBookingServiceImpl implements HotelBookingService {

    private final BookingProvider bookingProvider;
    private final HotelBookingRepository hotelBookingRepository;
    private final PatientCaseService caseService;
    private final ConsentService consentService;

    @Override
    public List<BookingProvider.HotelSearchResult> search(
            String city, LocalDate checkIn, LocalDate checkOut, int guests, String roomType) {
        return bookingProvider.searchHotels(new BookingProvider.HotelSearchCriteria(
            city, checkIn, checkOut, guests, roomType));
    }

    @Override
    public HotelBooking reserveOnBehalf(
            String caseId,
            String hotelProviderId,
            String roomType,
            BookingProvider.GuestInfo guest,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            String consentId) {

        consentService.assertApproved(consentId);
        PatientCase patientCase = caseService.findByIdOrThrow(caseId);

        BookingProvider.HotelBookingResult result = bookingProvider.reserveHotel(
            hotelProviderId, roomType, guest, checkIn, checkOut, guestCount);

        // Snapshot (in real impl, fetch hotel details by id)
        List<BookingProvider.HotelSearchResult> catalog = bookingProvider.searchHotels(
            new BookingProvider.HotelSearchCriteria(null, checkIn, checkOut, guestCount, roomType));
        BookingProvider.HotelSearchResult snap = catalog.stream()
            .filter(h -> h.providerId().equals(hotelProviderId))
            .findFirst()
            .orElse(catalog.isEmpty() ? null : catalog.get(0));

        HotelBooking hb = HotelBooking.builder()
            .caseId(caseId)
            .patientId(patientCase.getPatientId())
            .payloadHotelId(hotelProviderId)
            .hotelName(snap != null ? snap.name() : "—")
            .city(snap != null ? snap.city() : "—")
            .roomType(roomType)
            .guestFirstName(guest.firstName())
            .guestLastName(guest.lastName())
            .guestPassportNumber(guest.passportNumber())
            .guestPhone(guest.phone())
            .guestCount(guestCount)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .nights(result.nights())
            .pricePerNight(snap != null ? snap.pricePerNight() : null)
            .totalPrice(result.totalPrice())
            .currency(snap != null ? snap.currency() : "EUR")
            .status(HotelBooking.BookingStatus.MOCK_RESERVED)
            .confirmationNumber(result.confirmationNumber())
            .providerType(bookingProvider.getType())
            .bookedOnBehalf(Boolean.TRUE)
            .bookedByAgentId(SecurityUtils.getCurrentUserIdOrNull())
            .consentRecordId(consentId)
            .build();

        HotelBooking saved = hotelBookingRepository.save(hb);
        caseService.addHotelBooking(caseId, saved.getId(), saved.getTotalPrice());
        log.info("HotelBooking oluşturuldu: {} (case={}, conf={})", saved.getId(), caseId, saved.getConfirmationNumber());
        return saved;
    }

    @Override
    public List<HotelBooking> findByCase(String caseId) {
        return hotelBookingRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId);
    }
}
