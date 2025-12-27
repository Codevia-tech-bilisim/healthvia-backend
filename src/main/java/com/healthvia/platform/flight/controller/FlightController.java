// flight/controller/FlightController.java
package com.healthvia.platform.flight.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.flight.dto.FlightDto;
import com.healthvia.platform.flight.dto.FlightCreateRequest;
import com.healthvia.platform.flight.dto.FlightSearchRequest;
import com.healthvia.platform.flight.entity.Flight;
import com.healthvia.platform.flight.entity.Flight.CabinClass;
import com.healthvia.platform.flight.entity.Flight.FlightStatus;
import com.healthvia.platform.flight.service.FlightService;
import com.healthvia.platform.flight.service.FlightService.PopularRoute;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Slf4j
public class FlightController {

    private final FlightService flightService;

    // ===================================================================
    // PUBLIC ENDPOINTS - Herkes erişebilir
    // ===================================================================

    /**
     * Uçuş arama - Ana endpoint
     * Frontend'den gelen arama isteklerini karşılar
     */
    @GetMapping("/public/search")
    public ApiResponse<List<FlightDto>> searchFlights(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) CabinClass cabinClass,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean directOnly,
            @RequestParam(required = false) String airline) {
        
        log.info("Searching flights: {} -> {} on {}", departure, arrival, date);
        
        List<Flight> flights = flightService.searchFlights(departure, arrival, date);
        
        // Filtreleme
        if (cabinClass != null) {
            flights = flights.stream()
                .filter(f -> f.getCabinClass() == cabinClass)
                .toList();
        }
        if (maxPrice != null) {
            flights = flights.stream()
                .filter(f -> f.getTotalPrice() != null && f.getTotalPrice().compareTo(maxPrice) <= 0)
                .toList();
        }
        if (directOnly != null && directOnly) {
            flights = flights.stream()
                .filter(f -> f.getIsDirect() != null && f.getIsDirect())
                .toList();
        }
        if (airline != null && !airline.isEmpty()) {
            flights = flights.stream()
                .filter(f -> f.getAirline() != null && f.getAirline().toLowerCase().contains(airline.toLowerCase()))
                .toList();
        }
        
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos, flights.size() + " uçuş bulundu");
    }

    /**
     * Şehir bazlı uçuş arama
     */
    @GetMapping("/public/search/by-city")
    public ApiResponse<List<FlightDto>> searchFlightsByCity(
            @RequestParam String departureCity,
            @RequestParam String arrivalCity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Searching flights by city: {} -> {} on {}", departureCity, arrivalCity, date);
        
        List<Flight> flights = flightService.searchFlightsByCity(departureCity, arrivalCity, date);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * Tarih aralığında uçuş arama (esnek tarih)
     */
    @GetMapping("/public/search/flexible")
    public ApiResponse<List<FlightDto>> searchFlightsFlexible(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Searching flights flexible: {} -> {} from {} to {}", 
                 departure, arrival, startDate, endDate);
        
        List<Flight> flights = flightService.searchFlightsInDateRange(departure, arrival, startDate, endDate);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * Uçuş detayı
     */
    @GetMapping("/public/{id}")
    public ApiResponse<FlightDto> getFlightById(@PathVariable String id) {
        log.info("Getting flight by ID: {}", id);
        
        Flight flight = flightService.findById(id)
            .orElseThrow(() -> new RuntimeException("Uçuş bulunamadı: " + id));
        
        return ApiResponse.success(FlightDto.fromEntity(flight));
    }

    /**
     * Uçuş numarası ile arama
     */
    @GetMapping("/public/by-number")
    public ApiResponse<FlightDto> getFlightByNumber(
            @RequestParam String flightNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Flight flight = flightService.findByFlightNumberAndDate(flightNumber, date)
            .orElseThrow(() -> new RuntimeException("Uçuş bulunamadı: " + flightNumber));
        
        return ApiResponse.success(FlightDto.fromEntity(flight));
    }

    /**
     * Sadece direkt uçuşlar
     */
    @GetMapping("/public/direct")
    public ApiResponse<List<FlightDto>> getDirectFlights(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<Flight> flights = flightService.findDirectFlights(departure, arrival, date);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * En ucuz uçuşlar
     */
    @GetMapping("/public/cheapest")
    public ApiResponse<List<FlightDto>> getCheapestFlights(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "5") int limit) {
        
        List<Flight> flights = flightService.findCheapestFlights(departure, arrival, date, limit);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * Havayoluna göre uçuşlar
     */
    @GetMapping("/public/by-airline/{airlineCode}")
    public ApiResponse<List<FlightDto>> getFlightsByAirline(
            @PathVariable String airlineCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<Flight> flights = flightService.findByAirlineAndDate(airlineCode, date);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    // === SAĞLIK TURİZMİ ENDPOİNTLERİ ===

    /**
     * Sağlık turizmi dostu uçuşlar
     */
    @GetMapping("/public/medical-friendly")
    public ApiResponse<List<FlightDto>> getMedicalFriendlyFlights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<Flight> flights = flightService.findMedicalFriendlyFlights(date);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * Tekerlekli sandalye erişimli uçuşlar
     */
    @GetMapping("/public/wheelchair-accessible")
    public ApiResponse<List<FlightDto>> getWheelchairAccessibleFlights(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<Flight> flights = flightService.findWheelchairAccessibleFlights(departure, arrival, date);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * HealthVia partner uçuşlar (indirimli)
     */
    @GetMapping("/public/partners")
    public ApiResponse<List<FlightDto>> getPartnerFlights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        
        LocalDate effectiveDate = fromDate != null ? fromDate : LocalDate.now();
        List<Flight> flights = flightService.findHealthviaPartnerFlights(effectiveDate);
        List<FlightDto> flightDtos = flights.stream()
            .map(FlightDto::fromEntity)
            .toList();
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * Popüler rotalar
     */
    @GetMapping("/public/popular-routes")
    public ApiResponse<List<PopularRoute>> getPopularRoutes(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<PopularRoute> routes = flightService.getPopularRoutes(limit);
        return ApiResponse.success(routes);
    }

    // ===================================================================
    // ADMIN ENDPOINTS - Sadece adminler erişebilir
    // ===================================================================

    /**
     * Tüm uçuşları listele (sayfalı) - Admin
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<FlightDto>> getAllFlights(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Flight> flights = flightService.findAll(pageable);
        Page<FlightDto> flightDtos = flights.map(FlightDto::fromEntity);
        
        return ApiResponse.success(flightDtos);
    }

    /**
     * Yeni uçuş oluştur - Admin
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FlightDto> createFlight(@Valid @RequestBody FlightCreateRequest request) {
        log.info("Creating new flight: {} {}", request.getAirline(), request.getFlightNumber());
        
        Flight flight = request.toEntity();
        Flight createdFlight = flightService.createFlight(flight);
        
        return ApiResponse.success(FlightDto.fromEntity(createdFlight), "Uçuş başarıyla oluşturuldu");
    }

    /**
     * Uçuş güncelle - Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FlightDto> updateFlight(
            @PathVariable String id,
            @Valid @RequestBody FlightCreateRequest request) {
        
        log.info("Updating flight: {}", id);
        
        Flight flight = request.toEntity();
        Flight updatedFlight = flightService.updateFlight(id, flight);
        
        return ApiResponse.success(FlightDto.fromEntity(updatedFlight), "Uçuş başarıyla güncellendi");
    }

    /**
     * Uçuş sil (soft delete) - Admin
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteFlight(@PathVariable String id) {
        log.info("Deleting flight: {}", id);
        
        String deletedBy = SecurityUtils.getCurrentUserId();
        flightService.deleteFlight(id, deletedBy);
        
        return ApiResponse.success(null, "Uçuş başarıyla silindi");
    }

    /**
     * Uçuş durumunu güncelle - Admin
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FlightDto> updateFlightStatus(
            @PathVariable String id,
            @RequestParam FlightStatus status) {
        
        log.info("Updating flight {} status to {}", id, status);
        
        Flight flight = flightService.updateFlightStatus(id, status);
        
        return ApiResponse.success(FlightDto.fromEntity(flight), "Uçuş durumu güncellendi");
    }

    /**
     * Müsait koltuk sayısını güncelle - Admin
     */
    @PatchMapping("/{id}/available-seats")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FlightDto> updateAvailableSeats(
            @PathVariable String id,
            @RequestParam Integer seats) {
        
        Flight flight = flightService.updateAvailableSeats(id, seats);
        return ApiResponse.success(FlightDto.fromEntity(flight), "Koltuk sayısı güncellendi");
    }

    /**
     * Geçmiş uçuşların durumunu güncelle - Admin
     */
    @PostMapping("/admin/update-past-flights")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> updatePastFlightsStatus() {
        int count = flightService.updatePastFlightsStatus();
        return ApiResponse.success(count, count + " uçuş güncellendi");
    }

    // ===================================================================
    // STATISTICS ENDPOINTS - Admin
    // ===================================================================

    /**
     * İstatistikler - Admin
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FlightStatistics> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        
        FlightStatistics stats = new FlightStatistics();
        stats.setDate(effectiveDate);
        stats.setTotalFlights(flightService.countByDate(effectiveDate));
        
        return ApiResponse.success(stats);
    }

    // === Inner Classes ===
    
    @lombok.Data
    public static class FlightStatistics {
        private LocalDate date;
        private long totalFlights;
        private long scheduledFlights;
        private long delayedFlights;
        private long cancelledFlights;
    }
}
