package com.healthvia.platform.booking.flight.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.booking.flight.entity.FlightBooking;
import com.healthvia.platform.booking.flight.repository.FlightBookingRepository;
import com.healthvia.platform.booking.flight.service.FlightBookingService;
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
public class FlightBookingServiceImpl implements FlightBookingService {

    private final BookingProvider bookingProvider;
    private final FlightBookingRepository flightBookingRepository;
    private final PatientCaseService caseService;
    private final ConsentService consentService;

    @Override
    public List<BookingProvider.FlightSearchResult> search(
            String fromCode, String toCode, LocalDate departureDate,
            int passengers, String cabinClass) {
        return bookingProvider.searchFlights(new BookingProvider.FlightSearchCriteria(
            fromCode, toCode, departureDate, passengers, cabinClass));
    }

    @Override
    public FlightBooking reserveOnBehalf(
            String caseId,
            String flightProviderId,
            BookingProvider.PassengerInfo passenger,
            String consentId) {

        consentService.assertApproved(consentId);
        PatientCase patientCase = caseService.findByIdOrThrow(caseId);

        BookingProvider.FlightBookingResult result = bookingProvider.reserveFlight(flightProviderId, passenger);

        // For MVP we re-query search to snapshot pricing; in production, fetch from Payload by id.
        List<BookingProvider.FlightSearchResult> results = bookingProvider.searchFlights(
            new BookingProvider.FlightSearchCriteria(null, null, LocalDate.now(), 1, null));
        BookingProvider.FlightSearchResult snapshot = results.stream()
            .filter(r -> r.providerId().equals(flightProviderId))
            .findFirst()
            .orElse(results.isEmpty() ? null : results.get(0));

        FlightBooking fb = FlightBooking.builder()
            .caseId(caseId)
            .patientId(patientCase.getPatientId())
            .payloadFlightId(flightProviderId)
            .airline(snapshot != null ? snapshot.airline() : "—")
            .flightNumber(snapshot != null ? snapshot.flightNumber() : "—")
            .departureAirport(snapshot != null ? snapshot.departureAirport() : "—")
            .departureAirportCode(snapshot != null ? snapshot.departureAirportCode() : "—")
            .arrivalAirport(snapshot != null ? snapshot.arrivalAirport() : "—")
            .arrivalAirportCode(snapshot != null ? snapshot.arrivalAirportCode() : "—")
            .departureTime(snapshot != null ? snapshot.departureTime() : null)
            .arrivalTime(snapshot != null ? snapshot.arrivalTime() : null)
            .cabinClass(snapshot != null ? snapshot.cabinClass() : "ECONOMY")
            .passengerFirstName(passenger.firstName())
            .passengerLastName(passenger.lastName())
            .passengerPassportNumber(passenger.passportNumber())
            .passengerNationality(passenger.nationality())
            .passengerPhone(passenger.phone())
            .status(FlightBooking.BookingStatus.MOCK_RESERVED)
            .pnr(result.pnr())
            .providerType(bookingProvider.getType())
            .price(snapshot != null ? snapshot.price() : null)
            .currency(snapshot != null ? snapshot.currency() : "EUR")
            .bookedOnBehalf(Boolean.TRUE)
            .bookedByAgentId(SecurityUtils.getCurrentUserIdOrNull())
            .consentRecordId(consentId)
            .build();

        FlightBooking saved = flightBookingRepository.save(fb);
        caseService.addFlightBooking(caseId, saved.getId(), saved.getPrice());
        log.info("FlightBooking oluşturuldu: {} (case={}, pnr={})", saved.getId(), caseId, saved.getPnr());
        return saved;
    }

    @Override
    public List<FlightBooking> findByCase(String caseId) {
        return flightBookingRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId);
    }
}
