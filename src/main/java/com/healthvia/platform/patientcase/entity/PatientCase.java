package com.healthvia.platform.patientcase.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.model.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Patient journey container — a case groups together every resource (flights,
 * hotels, appointments, payments, tickets) that belong to a single patient's
 * treatment journey.
 *
 * A case is automatically created when a Lead transitions to CONVERTED, and
 * closes when the patient completes or cancels treatment.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "patient_cases")
@CompoundIndex(def = "{'status': 1, 'assignedAgentId': 1}")
@CompoundIndex(def = "{'patientId': 1, 'status': 1}")
public class PatientCase extends BaseEntity {

    @Indexed(unique = true)
    @Field("case_number")
    private String caseNumber;

    @Indexed
    @Field("patient_id")
    private String patientId;

    @Indexed
    @Field("lead_id")
    private String leadId;

    @Indexed
    @Field("assigned_agent_id")
    private String assignedAgentId;

    @Field("previous_agent_ids")
    private List<String> previousAgentIds;

    @Indexed
    private CaseStatus status;

    private CasePriority priority;

    @Field("treatment_type_id")
    private String treatmentTypeId;

    @Field("appointment_ids")
    private List<String> appointmentIds;

    @Field("flight_booking_ids")
    private List<String> flightBookingIds;

    @Field("hotel_booking_ids")
    private List<String> hotelBookingIds;

    @Field("payment_request_ids")
    private List<String> paymentRequestIds;

    @Field("ticket_ids")
    private List<String> ticketIds;

    @Field("conversation_id")
    private String conversationId;

    @Field("created_on_behalf")
    private Boolean createdOnBehalf;

    @Field("consent_record_ids")
    private List<String> consentRecordIds;

    @Field("total_amount")
    private BigDecimal totalAmount;

    @Field("paid_amount")
    private BigDecimal paidAmount;

    private String currency;

    private String notes;

    @Field("closed_at")
    private LocalDateTime closedAt;

    /* ----- Lifecycle helpers ----- */

    public void addAppointment(String appointmentId) {
        if (this.appointmentIds == null) this.appointmentIds = new ArrayList<>();
        if (!this.appointmentIds.contains(appointmentId)) this.appointmentIds.add(appointmentId);
    }

    public void addFlightBooking(String flightBookingId) {
        if (this.flightBookingIds == null) this.flightBookingIds = new ArrayList<>();
        if (!this.flightBookingIds.contains(flightBookingId)) this.flightBookingIds.add(flightBookingId);
    }

    public void addHotelBooking(String hotelBookingId) {
        if (this.hotelBookingIds == null) this.hotelBookingIds = new ArrayList<>();
        if (!this.hotelBookingIds.contains(hotelBookingId)) this.hotelBookingIds.add(hotelBookingId);
    }

    public void addPaymentRequest(String paymentRequestId) {
        if (this.paymentRequestIds == null) this.paymentRequestIds = new ArrayList<>();
        if (!this.paymentRequestIds.contains(paymentRequestId)) this.paymentRequestIds.add(paymentRequestId);
    }

    public void addConsent(String consentId) {
        if (this.consentRecordIds == null) this.consentRecordIds = new ArrayList<>();
        if (!this.consentRecordIds.contains(consentId)) this.consentRecordIds.add(consentId);
        this.createdOnBehalf = Boolean.TRUE;
    }

    public void addToTotal(BigDecimal amount) {
        if (amount == null) return;
        this.totalAmount = (this.totalAmount == null ? BigDecimal.ZERO : this.totalAmount).add(amount);
    }

    public void registerPayment(BigDecimal amount) {
        if (amount == null) return;
        this.paidAmount = (this.paidAmount == null ? BigDecimal.ZERO : this.paidAmount).add(amount);
    }

    public void close() {
        this.status = CaseStatus.COMPLETED;
        this.closedAt = LocalDateTime.now();
    }

    public enum CaseStatus {
        OPEN,
        IN_PROGRESS,
        AWAITING_PAYMENT,
        PAID,
        IN_TREATMENT,
        COMPLETED,
        CANCELLED
    }

    public enum CasePriority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
