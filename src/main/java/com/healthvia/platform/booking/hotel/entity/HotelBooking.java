package com.healthvia.platform.booking.hotel.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "hotel_bookings")
public class HotelBooking extends BaseEntity {

    @Indexed @Field("case_id") private String caseId;
    @Indexed @Field("patient_id") private String patientId;

    @Field("payload_hotel_id") private String payloadHotelId;

    @Field("hotel_name") private String hotelName;
    private String city;

    @Field("room_type") private String roomType;

    @Field("guest_first_name") private String guestFirstName;
    @Field("guest_last_name") private String guestLastName;
    @Field("guest_passport") private String guestPassportNumber;
    @Field("guest_phone") private String guestPhone;
    @Field("guest_count") private Integer guestCount;

    @Field("check_in") private LocalDate checkIn;
    @Field("check_out") private LocalDate checkOut;
    private Integer nights;

    @Field("price_per_night") private BigDecimal pricePerNight;
    @Field("total_price") private BigDecimal totalPrice;
    private String currency;

    @Indexed private BookingStatus status;
    @Field("confirmation_number") private String confirmationNumber;
    @Field("provider_type") private BookingProvider.BookingProviderType providerType;

    @Field("booked_on_behalf") private Boolean bookedOnBehalf;
    @Field("booked_by_agent_id") private String bookedByAgentId;
    @Field("consent_record_id") private String consentRecordId;

    public enum BookingStatus {
        MOCK_RESERVED, CONFIRMED, CANCELLED, PENDING_PAYMENT
    }
}
