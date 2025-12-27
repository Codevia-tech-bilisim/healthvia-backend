// flight/service/impl/FlightServiceImpl.java
package com.healthvia.platform.flight.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.flight.entity.Flight;
import com.healthvia.platform.flight.entity.Flight.CabinClass;
import com.healthvia.platform.flight.entity.Flight.FlightStatus;
import com.healthvia.platform.flight.repository.FlightRepository;
import com.healthvia.platform.flight.service.FlightService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    // === CRUD İŞLEMLERİ ===

    @Override
    public Flight createFlight(Flight flight) {
        log.info("Creating new flight: {} - {}", flight.getAirline(), flight.getFlightNumber());
        
        // Tarihleri hesapla
        flight.calculateDatetimes();
        
        // Varsayılan değerler
        if (flight.getStatus() == null) {
            flight.setStatus(FlightStatus.SCHEDULED);
        }
        if (flight.getCabinClass() == null) {
            flight.setCabinClass(CabinClass.ECONOMY);
        }
        if (flight.getIsDirect() == null) {
            flight.setIsDirect(true);
            flight.setStopsCount(0);
        }
        
        // Toplam fiyatı hesapla
        if (flight.getTotalPrice() == null && flight.getBasePrice() != null) {
            BigDecimal taxes = flight.getTaxesAndFees() != null ? flight.getTaxesAndFees() : BigDecimal.ZERO;
            flight.setTotalPrice(flight.getBasePrice().add(taxes));
        }
        
        return flightRepository.save(flight);
    }

    @Override
    public Flight updateFlight(String id, Flight flightData) {
        log.info("Updating flight: {}", id);
        
        Flight existingFlight = findByIdOrThrow(id);
        
        // Güncellenebilir alanları kopyala
        if (flightData.getAirline() != null) existingFlight.setAirline(flightData.getAirline());
        if (flightData.getFlightNumber() != null) existingFlight.setFlightNumber(flightData.getFlightNumber());
        if (flightData.getDepartureAirportCode() != null) existingFlight.setDepartureAirportCode(flightData.getDepartureAirportCode());
        if (flightData.getArrivalAirportCode() != null) existingFlight.setArrivalAirportCode(flightData.getArrivalAirportCode());
        if (flightData.getDepartureDate() != null) existingFlight.setDepartureDate(flightData.getDepartureDate());
        if (flightData.getDepartureTime() != null) existingFlight.setDepartureTime(flightData.getDepartureTime());
        if (flightData.getArrivalDate() != null) existingFlight.setArrivalDate(flightData.getArrivalDate());
        if (flightData.getArrivalTime() != null) existingFlight.setArrivalTime(flightData.getArrivalTime());
        if (flightData.getBasePrice() != null) existingFlight.setBasePrice(flightData.getBasePrice());
        if (flightData.getTotalPrice() != null) existingFlight.setTotalPrice(flightData.getTotalPrice());
        if (flightData.getCabinClass() != null) existingFlight.setCabinClass(flightData.getCabinClass());
        if (flightData.getAvailableSeats() != null) existingFlight.setAvailableSeats(flightData.getAvailableSeats());
        if (flightData.getIsDirect() != null) existingFlight.setIsDirect(flightData.getIsDirect());
        
        // Tarihleri yeniden hesapla
        existingFlight.calculateDatetimes();
        
        return flightRepository.save(existingFlight);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Flight> findById(String id) {
        return flightRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Flight> findByFlightNumberAndDate(String flightNumber, LocalDate date) {
        return flightRepository.findByFlightNumberAndDepartureDateAndDeletedFalse(flightNumber, date);
    }

    @Override
    public void deleteFlight(String id, String deletedBy) {
        log.info("Soft deleting flight: {} by user: {}", id, deletedBy);
        
        Flight flight = findByIdOrThrow(id);
        flight.setDeleted(true);
        flight.setDeletedAt(LocalDateTime.now());
        flight.setDeletedBy(deletedBy);
        
        flightRepository.save(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Flight> findAll(Pageable pageable) {
        return flightRepository.findByDeletedFalse(pageable);
    }

    // === UÇUŞ ARAMA ===

    @Override
    @Transactional(readOnly = true)
    public List<Flight> searchFlights(String departureCode, String arrivalCode, LocalDate date) {
        log.debug("Searching flights: {} -> {} on {}", departureCode, arrivalCode, date);
        return flightRepository.findFlightsByRoute(departureCode, arrivalCode, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> searchFlightsByCity(String departureCity, String arrivalCity, LocalDate date) {
        log.debug("Searching flights by city: {} -> {} on {}", departureCity, arrivalCity, date);
        return flightRepository.findFlightsByCity(departureCity, arrivalCity, date);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Flight> searchFlightsWithFilters(
            String departureCode,
            String arrivalCode,
            LocalDate date,
            CabinClass cabinClass,
            BigDecimal maxPrice,
            Boolean directOnly,
            String airline,
            Pageable pageable) {
        
        log.debug("Searching flights with filters - route: {} -> {}, date: {}, class: {}, maxPrice: {}", 
                  departureCode, arrivalCode, date, cabinClass, maxPrice);
        
        // Varsayılan değerler
        BigDecimal effectiveMaxPrice = maxPrice != null ? maxPrice : BigDecimal.valueOf(999999);
        Boolean effectiveDirectOnly = directOnly != null ? directOnly : false;
        
        return flightRepository.searchFlights(
            departureCode, arrivalCode, date, cabinClass, effectiveMaxPrice, effectiveDirectOnly, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> searchFlightsInDateRange(
            String departureCode, 
            String arrivalCode, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        return flightRepository.findFlightsByRouteAndDateRange(departureCode, arrivalCode, startDate, endDate);
    }

    // === DİREKT / AKTARMALI ===

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findDirectFlights(String departureCode, String arrivalCode, LocalDate date) {
        return flightRepository.findDirectFlights(departureCode, arrivalCode, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findConnectingFlights(String departureCode, String arrivalCode, LocalDate date) {
        return flightRepository.findConnectingFlights(departureCode, arrivalCode, date);
    }

    // === HAVAYOLU BAZLI ===

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findByAirline(String airline) {
        return flightRepository.findByAirlineAndDeletedFalse(airline);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findByAirlineAndDate(String airlineCode, LocalDate date) {
        return flightRepository.findByAirlineCodeAndDepartureDateAndDeletedFalse(airlineCode, date);
    }

    // === FİYAT BAZLI ===

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findByMaxPrice(BigDecimal maxPrice, LocalDate date) {
        return flightRepository.findByMaxPrice(maxPrice, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findCheapestFlights(String departureCode, String arrivalCode, LocalDate date, int limit) {
        List<Flight> flights = flightRepository.findFlightsByRoute(departureCode, arrivalCode, date);
        
        return flights.stream()
            .filter(f -> f.getTotalPrice() != null)
            .sorted(Comparator.comparing(Flight::getTotalPrice))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // === SAĞLIK TURİZMİ ===

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findMedicalFriendlyFlights(LocalDate date) {
        return flightRepository.findMedicalFriendlyFlights(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findWheelchairAccessibleFlights(String departureCode, String arrivalCode, LocalDate date) {
        return flightRepository.findWheelchairAccessibleFlights(departureCode, arrivalCode, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findStretcherAvailableFlights(LocalDate fromDate) {
        return flightRepository.findStretcherAvailableFlights(fromDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> findHealthviaPartnerFlights(LocalDate fromDate) {
        return flightRepository.findHealthviaPartnerFlights(fromDate);
    }

    // === DURUM YÖNETİMİ ===

    @Override
    public Flight updateFlightStatus(String flightId, FlightStatus status) {
        log.info("Updating flight {} status to {}", flightId, status);
        
        Flight flight = findByIdOrThrow(flightId);
        flight.setStatus(status);
        
        return flightRepository.save(flight);
    }

    @Override
    public int updatePastFlightsStatus() {
        log.info("Updating status for past flights");
        
        LocalDate today = LocalDate.now();
        List<Flight> pastFlights = flightRepository.findPastFlightsNeedingStatusUpdate(today);
        
        int count = 0;
        for (Flight flight : pastFlights) {
            flight.setStatus(FlightStatus.ARRIVED);
            flightRepository.save(flight);
            count++;
        }
        
        log.info("Updated {} past flights to ARRIVED status", count);
        return count;
    }

    // === KOLTUK YÖNETİMİ ===

    @Override
    public Flight updateAvailableSeats(String flightId, Integer availableSeats) {
        Flight flight = findByIdOrThrow(flightId);
        flight.setAvailableSeats(availableSeats);
        return flightRepository.save(flight);
    }

    @Override
    public Flight reserveSeats(String flightId, Integer seatCount) {
        log.info("Reserving {} seats for flight {}", seatCount, flightId);
        
        Flight flight = findByIdOrThrow(flightId);
        
        if (flight.getAvailableSeats() == null || flight.getAvailableSeats() < seatCount) {
            throw new RuntimeException("Yeterli koltuk yok. Mevcut: " + 
                (flight.getAvailableSeats() != null ? flight.getAvailableSeats() : 0));
        }
        
        flight.setAvailableSeats(flight.getAvailableSeats() - seatCount);
        return flightRepository.save(flight);
    }

    @Override
    public Flight releaseSeats(String flightId, Integer seatCount) {
        log.info("Releasing {} seats for flight {}", seatCount, flightId);
        
        Flight flight = findByIdOrThrow(flightId);
        
        int currentSeats = flight.getAvailableSeats() != null ? flight.getAvailableSeats() : 0;
        flight.setAvailableSeats(currentSeats + seatCount);
        
        return flightRepository.save(flight);
    }

    // === POPÜLER ROTALAR ===

    @Override
    @Transactional(readOnly = true)
    public List<PopularRoute> getPopularRoutes(int limit) {
        // Bu basit bir implementasyon - gerçek projede aggregation kullanılmalı
        List<Flight> allFlights = flightRepository.findAllForRouteAnalysis();
        
        return allFlights.stream()
            .collect(Collectors.groupingBy(
                f -> f.getDepartureAirportCode() + "-" + f.getArrivalAirportCode(),
                Collectors.toList()))
            .entrySet().stream()
            .map(entry -> {
                List<Flight> flights = entry.getValue();
                Flight sample = flights.get(0);
                
                BigDecimal avgPrice = flights.stream()
                    .filter(f -> f.getTotalPrice() != null)
                    .map(Flight::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(flights.size()), 2, java.math.RoundingMode.HALF_UP);
                
                return new PopularRoute(
                    sample.getDepartureAirportCode(),
                    sample.getDepartureCity(),
                    sample.getArrivalAirportCode(),
                    sample.getArrivalCity(),
                    flights.size(),
                    avgPrice
                );
            })
            .sorted((a, b) -> Long.compare(b.getFlightCount(), a.getFlightCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // === İSTATİSTİKLER ===

    @Override
    @Transactional(readOnly = true)
    public long countByDate(LocalDate date) {
        return flightRepository.countByDepartureDateAndDeletedFalse(date);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAirline(String airline, LocalDate date) {
        return flightRepository.countByAirlineAndDepartureDateAndDeletedFalse(airline, date);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeparturesByAirport(String airportCode, LocalDate date) {
        return flightRepository.countByDepartureAirportCodeAndDepartureDateAndDeletedFalse(airportCode, date);
    }

    // === HELPER METHODS ===

    private Flight findByIdOrThrow(String id) {
        return flightRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
    }
}
