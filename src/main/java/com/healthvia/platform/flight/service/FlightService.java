// flight/service/FlightService.java
package com.healthvia.platform.flight.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.flight.entity.Flight;
import com.healthvia.platform.flight.entity.Flight.CabinClass;
import com.healthvia.platform.flight.entity.Flight.FlightStatus;

public interface FlightService {

    // === CRUD İŞLEMLERİ ===
    
    /**
     * Yeni uçuş oluştur
     */
    Flight createFlight(Flight flight);
    
    /**
     * Uçuş güncelle
     */
    Flight updateFlight(String id, Flight flight);
    
    /**
     * Uçuş bul (ID ile)
     */
    Optional<Flight> findById(String id);
    
    /**
     * Uçuş numarası ve tarih ile bul
     */
    Optional<Flight> findByFlightNumberAndDate(String flightNumber, LocalDate date);
    
    /**
     * Uçuş sil (soft delete)
     */
    void deleteFlight(String id, String deletedBy);
    
    /**
     * Tüm uçuşları listele (sayfalı)
     */
    Page<Flight> findAll(Pageable pageable);

    // === UÇUŞ ARAMA ===
    
    /**
     * Rota bazlı uçuş arama (tek yön)
     */
    List<Flight> searchFlights(String departureCode, String arrivalCode, LocalDate date);
    
    /**
     * Şehir bazlı uçuş arama
     */
    List<Flight> searchFlightsByCity(String departureCity, String arrivalCity, LocalDate date);
    
    /**
     * Gelişmiş uçuş arama (filtreli)
     */
    Page<Flight> searchFlightsWithFilters(
        String departureCode,
        String arrivalCode,
        LocalDate date,
        CabinClass cabinClass,
        BigDecimal maxPrice,
        Boolean directOnly,
        String airline,
        Pageable pageable
    );
    
    /**
     * Tarih aralığında uçuş arama
     */
    List<Flight> searchFlightsInDateRange(
        String departureCode, 
        String arrivalCode, 
        LocalDate startDate, 
        LocalDate endDate
    );

    // === DİREKT / AKTARMALI ===
    
    /**
     * Sadece direkt uçuşları getir
     */
    List<Flight> findDirectFlights(String departureCode, String arrivalCode, LocalDate date);
    
    /**
     * Sadece aktarmalı uçuşları getir
     */
    List<Flight> findConnectingFlights(String departureCode, String arrivalCode, LocalDate date);

    // === HAVAYOLU BAZLI ===
    
    /**
     * Havayoluna göre uçuşlar
     */
    List<Flight> findByAirline(String airline);
    
    /**
     * Havayolu ve tarih ile uçuşlar
     */
    List<Flight> findByAirlineAndDate(String airlineCode, LocalDate date);

    // === FİYAT BAZLI ===
    
    /**
     * Maksimum fiyata göre uçuşlar
     */
    List<Flight> findByMaxPrice(BigDecimal maxPrice, LocalDate date);
    
    /**
     * En ucuz uçuşları getir
     */
    List<Flight> findCheapestFlights(String departureCode, String arrivalCode, LocalDate date, int limit);

    // === SAĞLIK TURİZMİ ===
    
    /**
     * Sağlık turizmi dostu uçuşlar
     */
    List<Flight> findMedicalFriendlyFlights(LocalDate date);
    
    /**
     * Tekerlekli sandalye erişimli uçuşlar
     */
    List<Flight> findWheelchairAccessibleFlights(String departureCode, String arrivalCode, LocalDate date);
    
    /**
     * Sedye taşıma imkanı olan uçuşlar
     */
    List<Flight> findStretcherAvailableFlights(LocalDate fromDate);
    
    /**
     * HealthVia partner uçuşlar (indirimli)
     */
    List<Flight> findHealthviaPartnerFlights(LocalDate fromDate);

    // === DURUM YÖNETİMİ ===
    
    /**
     * Uçuş durumunu güncelle
     */
    Flight updateFlightStatus(String flightId, FlightStatus status);
    
    /**
     * Toplu durum güncelleme (geçmiş uçuşlar için)
     */
    int updatePastFlightsStatus();

    // === KOLTUK YÖNETİMİ ===
    
    /**
     * Müsait koltuk sayısını güncelle
     */
    Flight updateAvailableSeats(String flightId, Integer availableSeats);
    
    /**
     * Koltuk rezerve et
     */
    Flight reserveSeats(String flightId, Integer seatCount);
    
    /**
     * Koltuk rezervasyonunu iptal et
     */
    Flight releaseSeats(String flightId, Integer seatCount);

    // === POPÜLER ROTALAR ===
    
    /**
     * En popüler rotaları getir
     */
    List<PopularRoute> getPopularRoutes(int limit);

    // === İSTATİSTİKLER ===
    
    /**
     * Tarih bazlı uçuş sayısı
     */
    long countByDate(LocalDate date);
    
    /**
     * Havayolu bazlı uçuş sayısı
     */
    long countByAirline(String airline, LocalDate date);
    
    /**
     * Havalimanı bazlı kalkış sayısı
     */
    long countDeparturesByAirport(String airportCode, LocalDate date);

    // === HELPER CLASSES ===
    
    @lombok.Data
    @lombok.AllArgsConstructor
    class PopularRoute {
        private String departureCode;
        private String departureCity;
        private String arrivalCode;
        private String arrivalCity;
        private long flightCount;
        private BigDecimal averagePrice;
    }
}
