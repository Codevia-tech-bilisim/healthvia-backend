// flight/repository/FlightRepository.java
package com.healthvia.platform.flight.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.flight.entity.Flight;
import com.healthvia.platform.flight.entity.Flight.CabinClass;
import com.healthvia.platform.flight.entity.Flight.FlightStatus;

@Repository
public interface FlightRepository extends MongoRepository<Flight, String> {

    // === TEMEL SORGULAR ===
    
    Optional<Flight> findByIdAndDeletedFalse(String id);
    
    Optional<Flight> findByFlightNumberAndDepartureDateAndDeletedFalse(String flightNumber, LocalDate departureDate);
    
    List<Flight> findByStatusAndDeletedFalse(FlightStatus status);
    
    Page<Flight> findByDeletedFalse(Pageable pageable);

    // === ROTA BAZLI SORGULAR ===
    
    @Query("{ 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': ?2, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findFlightsByRoute(String departureCode, String arrivalCode, LocalDate date);
    
    @Query("{ 'departureCity': ?0, 'arrivalCity': ?1, 'departureDate': ?2, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findFlightsByCity(String departureCity, String arrivalCity, LocalDate date);
    
    Page<Flight> findByDepartureAirportCodeAndArrivalAirportCodeAndDepartureDateAndDeletedFalse(
        String departureCode, String arrivalCode, LocalDate date, Pageable pageable);

    // === TARİH BAZLI SORGULAR ===
    
    List<Flight> findByDepartureDateAndDeletedFalse(LocalDate date);
    
    @Query("{ 'departureDate': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Flight> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("{ 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': { $gte: ?2, $lte: ?3 }, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findFlightsByRouteAndDateRange(String departureCode, String arrivalCode, LocalDate startDate, LocalDate endDate);

    // === HAVAYOLU BAZLI SORGULAR ===
    
    List<Flight> findByAirlineAndDeletedFalse(String airline);
    
    List<Flight> findByAirlineCodeAndDepartureDateAndDeletedFalse(String airlineCode, LocalDate date);
    
    @Query("{ 'airline': ?0, 'departureAirportCode': ?1, 'departureDate': ?2, 'deleted': false }")
    List<Flight> findByAirlineAndDepartureAndDate(String airline, String departureCode, LocalDate date);

    // === FİYAT BAZLI SORGULAR ===
    
    @Query("{ 'totalPrice': { $lte: ?0 }, 'departureDate': ?1, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findByMaxPrice(BigDecimal maxPrice, LocalDate date);
    
    @Query("{ 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': ?2, 'totalPrice': { $lte: ?3 }, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findFlightsByRouteAndMaxPrice(String departureCode, String arrivalCode, LocalDate date, BigDecimal maxPrice);

    // === KABİN SINIFI SORGULARI ===
    
    @Query("{ 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': ?2, 'cabinClass': ?3, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findFlightsByRouteAndCabinClass(String departureCode, String arrivalCode, LocalDate date, CabinClass cabinClass);

    // === DİREKT / AKTARMALI ===
    
    @Query("{ 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': ?2, 'isDirect': true, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findDirectFlights(String departureCode, String arrivalCode, LocalDate date);
    
    @Query("{ 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': ?2, 'isDirect': false, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findConnectingFlights(String departureCode, String arrivalCode, LocalDate date);

    // === SAĞLIK TURİZMİ ===
    
    @Query("{ 'isMedicalTravelFriendly': true, 'departureDate': ?0, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findMedicalFriendlyFlights(LocalDate date);
    
    @Query("{ 'wheelchairAssistance': true, 'departureAirportCode': ?0, 'arrivalAirportCode': ?1, 'departureDate': ?2, 'deleted': false }")
    List<Flight> findWheelchairAccessibleFlights(String departureCode, String arrivalCode, LocalDate date);
    
    @Query("{ 'stretcherAvailable': true, 'departureDate': { $gte: ?0 }, 'deleted': false }")
    List<Flight> findStretcherAvailableFlights(LocalDate fromDate);

    // === HEALTHVIA PARTNER ===
    
    @Query("{ 'isHealthviaPartner': true, 'departureDate': { $gte: ?0 }, 'status': { $in: ['SCHEDULED', 'ON_TIME'] }, 'deleted': false }")
    List<Flight> findHealthviaPartnerFlights(LocalDate fromDate);
    
    @Query("{ 'isHealthviaPartner': true, 'partnerDiscountPercentage': { $gt: 0 }, 'deleted': false }")
    List<Flight> findFlightsWithPartnerDiscount();

    // === GELİŞMİŞ ARAMA ===
    
    @Query("{ " +
           "'departureAirportCode': ?0, " +
           "'arrivalAirportCode': ?1, " +
           "'departureDate': ?2, " +
           "'status': { $in: ['SCHEDULED', 'ON_TIME'] }, " +
           "'deleted': false, " +
           "$or: [ " +
           "  { 'cabinClass': { $exists: false } }, " +
           "  { 'cabinClass': ?3 } " +
           "], " +
           "$or: [ " +
           "  { 'totalPrice': { $exists: false } }, " +
           "  { 'totalPrice': { $lte: ?4 } } " +
           "], " +
           "$or: [ " +
           "  { 'isDirect': { $exists: false } }, " +
           "  { 'isDirect': ?5 } " +
           "] " +
           "}")
    Page<Flight> searchFlights(String departureCode, String arrivalCode, LocalDate date, 
                               CabinClass cabinClass, BigDecimal maxPrice, Boolean directOnly,
                               Pageable pageable);

    // === DURUM GÜNCELLEMELERİ ===
    
    List<Flight> findByDepartureDateAndStatusAndDeletedFalse(LocalDate date, FlightStatus status);
    
    @Query("{ 'departureDate': { $lt: ?0 }, 'status': { $in: ['SCHEDULED', 'ON_TIME', 'DELAYED'] }, 'deleted': false }")
    List<Flight> findPastFlightsNeedingStatusUpdate(LocalDate date);

    // === İSTATİSTİKLER ===
    
    long countByDepartureDateAndDeletedFalse(LocalDate date);
    
    long countByAirlineAndDepartureDateAndDeletedFalse(String airline, LocalDate date);
    
    long countByDepartureAirportCodeAndDepartureDateAndDeletedFalse(String airportCode, LocalDate date);

    // === POPÜLER ROTALAR ===
    
    @Query(value = "{ 'deleted': false }", 
           sort = "{ 'departureAirportCode': 1, 'arrivalAirportCode': 1 }")
    List<Flight> findAllForRouteAnalysis();
}
