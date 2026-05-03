package com.healthvia.platform.patientcase.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * End-to-end financial picture of a single patient case — fed to the case
 * detail screen so the agent + CEO see exactly where the money flows: each
 * booked flight + hotel, every appointment fee, every payment request and
 * the balance.
 *
 * Built by aggregating FlightBooking / HotelBooking / Appointment /
 * PaymentRequest records belonging to the case. Empty if the case has none.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseFinancialSummaryDto {

    private String caseId;
    private String caseNumber;
    private String currency;

    // Totals
    private BigDecimal totalAmount;       // Sum of all charges (bookings + appointments)
    private BigDecimal paidAmount;        // Sum of PAID payment requests
    private BigDecimal pendingAmount;     // Sum of LINK_SENT / PENDING payment requests
    private BigDecimal balance;           // totalAmount - paidAmount

    // Time-on-platform (createdAt → closedAt or now)
    private LocalDateTime journeyStartedAt;
    private LocalDateTime journeyClosedAt;
    private Long journeyDurationDays;

    // Component breakdown
    private List<LineItem> flights;
    private List<LineItem> hotels;
    private List<LineItem> appointments;
    private List<PaymentLine> payments;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LineItem {
        private String id;
        private String description;
        private BigDecimal amount;
        private String currency;
        private String status;
        private LocalDateTime occurredAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentLine {
        private String id;
        private String method;        // LINK / AGENT_ASSISTED / BANK_TRANSFER
        private String status;        // PAID / LINK_SENT / PENDING / EXPIRED
        private BigDecimal amount;
        private String currency;
        private String description;
        private String linkUrl;
        private LocalDateTime sentAt;
        private LocalDateTime paidAt;
        private LocalDateTime expiresAt;
    }
}
