package com.healthvia.platform.booking.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.booking.provider.BookingProvider;
import com.healthvia.platform.common.model.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Reservation record (≠ inventory). A FlightBooking captures the snapshot of
 * the flight as it existed at booking time, plus passenger info, AOBO
 * linkage, PNR from the provider and status.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "flight_bookings")
public class FlightBooking extends BaseEntity {

    @Indexed @Field("case_id") private String caseId;
    @Indexed @Field("patient_id") private String patientId;

    @Field("payload_flight_id")
    private String payloadFlightId;

    private String airline;
    @Field("flight_number") private String flightNumber;
    @Field("departure_airport") private String departureAirport;
    @Field("departure_airport_code") private String departureAirportCode;
    @Field("arrival_airport") private String arrivalAirport;
    @Field("arrival_airport_code") private String arrivalAirportCode;
    @Field("departure_time") private LocalDateTime departureTime;
    @Field("arrival_time") private LocalDateTime arrivalTime;
    @Field("cabin_class") private String cabinClass;

    @Field("passenger_first_name") private String passengerFirstName;
    @Field("passenger_last_name") private String passengerLastName;
    @Field("passenger_passport") private String passengerPassportNumber;
    @Field("passenger_nationality") private String passengerNationality;
    @Field("passenger_phone") private String passengerPhone;

    @Indexed private BookingStatus status;
    private String pnr;
    @Field("provider_type") private BookingProvider.BookingProviderType providerType;

    private BigDecimal price;
    private String currency;

    @Field("booked_on_behalf") private Boolean bookedOnBehalf;
    @Field("booked_by_agent_id") private String bookedByAgentId;
    @Field("consent_record_id") private String consentRecordId;

    public enum BookingStatus {
        MOCK_RESERVED, CONFIRMED, CANCELLED, PENDING_PAYMENT
    }
}
