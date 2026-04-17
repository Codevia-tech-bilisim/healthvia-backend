package com.healthvia.platform.booking.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Default booking provider: returns deterministic mock inventory. Production
 * will replace this with `AmadeusBookingProvider` once the flight license is
 * signed. No travel operations fail because of a missing vendor.
 *
 * Long term: read real Flights/Hotels inventory from Payload CMS via REST.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "booking.provider", havingValue = "mock", matchIfMissing = true)
public class MockBookingProvider implements BookingProvider {

    private static final Random RANDOM = new Random(42);

    @Override
    public BookingProviderType getType() {
        return BookingProviderType.MOCK;
    }

    @Override
    public List<FlightSearchResult> searchFlights(FlightSearchCriteria c) {
        LocalDate date = c.departureDate() != null ? c.departureDate() : LocalDate.now().plusDays(7);
        return List.of(
            flight("TK1980", "Turkish Airlines", c.fromCode(), c.toCode(), date, 9, 15, BigDecimal.valueOf(310), c.cabinClass()),
            flight("PC1238", "Pegasus", c.fromCode(), c.toCode(), date, 11, 17, BigDecimal.valueOf(180), c.cabinClass()),
            flight("TK1984", "Turkish Airlines", c.fromCode(), c.toCode(), date, 18, 23, BigDecimal.valueOf(285), c.cabinClass()),
            flight("VF0712", "AJet", c.fromCode(), c.toCode(), date, 7, 13, BigDecimal.valueOf(165), c.cabinClass()));
    }

    @Override
    public FlightBookingResult reserveFlight(String flightProviderId, PassengerInfo passenger) {
        String pnr = "MOCK-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        log.info("[MOCK] Flight reserved: providerId={}, passenger={} {}, pnr={}",
            flightProviderId, passenger.firstName(), passenger.lastName(), pnr);
        return new FlightBookingResult(flightProviderId, pnr, "MOCK_RESERVED");
    }

    @Override
    public List<HotelSearchResult> searchHotels(HotelSearchCriteria c) {
        BigDecimal base = BigDecimal.valueOf(90 + RANDOM.nextInt(120));
        return List.of(
            hotel("mock-htl-1", "Swissotel The Bosphorus", c.city(), 5, 4.8, base.add(BigDecimal.valueOf(140)),
                "Memorial Şişli", 4.2),
            hotel("mock-htl-2", "Hilton Istanbul Bomonti", c.city(), 5, 4.7, base.add(BigDecimal.valueOf(70)),
                "Memorial Şişli", 0.9),
            hotel("mock-htl-3", "Radisson Blu Asia", c.city(), 5, 4.4, base.subtract(BigDecimal.valueOf(5)),
                "Memorial Ataşehir", 1.2),
            hotel("mock-htl-4", "Holiday Inn Kadıköy", c.city(), 4, 4.3, base.subtract(BigDecimal.valueOf(30)),
                "Acıbadem Kadıköy", 1.8));
    }

    @Override
    public HotelBookingResult reserveHotel(
            String hotelProviderId,
            String roomType,
            GuestInfo guest,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount) {
        int nights = (int) Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
        BigDecimal perNight = BigDecimal.valueOf(120 + RANDOM.nextInt(80));
        BigDecimal total = perNight.multiply(BigDecimal.valueOf(nights));
        String conf = "MOCK-H" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        log.info("[MOCK] Hotel reserved: providerId={}, room={}, nights={}, total={}, conf={}",
            hotelProviderId, roomType, nights, total, conf);
        return new HotelBookingResult(hotelProviderId, conf, "MOCK_RESERVED", total, nights);
    }

    /* ----- builders ----- */

    private FlightSearchResult flight(
            String no, String airline, String fromCode, String toCode,
            LocalDate date, int depHour, int arrHour, BigDecimal price, String cabin) {
        LocalDateTime dep = date.atTime(depHour, 15);
        LocalDateTime arr = date.atTime(arrHour, 30);
        String dur = (arrHour - depHour) + "h 15m";
        return new FlightSearchResult(
            "mock-" + no,
            airline,
            no,
            toAirportName(fromCode),
            fromCode,
            toAirportName(toCode),
            toCode,
            dep, arr, dur, 0,
            cabin == null ? "ECONOMY" : cabin,
            price, "EUR",
            3 + RANDOM.nextInt(15));
    }

    private HotelSearchResult hotel(
            String id, String name, String city, int star, double rating,
            BigDecimal price, String nearestHospital, double distance) {
        return new HotelSearchResult(
            id, name, city != null ? city : "İstanbul", city + " city center",
            star, rating, price, "EUR", nearestHospital, distance);
    }

    private String toAirportName(String code) {
        if (code == null) return "—";
        return switch (code.toUpperCase()) {
            case "IST" -> "Istanbul Airport";
            case "SAW" -> "Sabiha Gökçen";
            case "LHR" -> "Heathrow";
            case "LGW" -> "Gatwick";
            case "STN" -> "Stansted";
            case "FRA" -> "Frankfurt";
            case "CDG" -> "Charles de Gaulle";
            case "JFK" -> "JFK";
            case "DXB" -> "Dubai Intl";
            default -> code + " Airport";
        };
    }
}
