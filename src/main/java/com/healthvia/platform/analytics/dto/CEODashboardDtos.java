package com.healthvia.platform.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CEODashboardDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Overview {
        private Revenue revenue;
        private Leads leads;
        private Operations operations;
        private List<LostReason> lostReasons;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Revenue {
        private BigDecimal total;
        private String currency;
        private BigDecimal previousPeriod;
        private double changePercent;
        private BigDecimal pipelineValue;
        private BigDecimal averageRevenuePerPatient;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Leads {
        private long total;
        private long newToday;
        private long converted;
        private long lost;
        private double conversionRate;
        private Map<String, Long> byStatus;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Operations {
        private long activeLeads;
        private long openTickets;
        private long slaBreaches;
        private long pendingPayments;
        private BigDecimal pendingPaymentsAmount;
        private long upcomingAppointments7d;
        private double avgFirstResponseMinutes;
        private double avgResolutionHours;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LostReason {
        private String reason;
        private long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RevenueTimeseriesPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private long leads;
        private long converted;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PipelineFunnelPoint {
        private String stage;
        private long count;
        private BigDecimal value;
        private double conversionFromPrevious;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AgentPerformance {
        private String agentId;
        private String agentName;
        private String avatarUrl;
        private long leadsHandled;
        private long leadsConverted;
        private double conversionRate;
        private BigDecimal revenue;
        private double avgResponseMinutes;
        private double occupancy;
        private int ranking;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChannelBreakdownPoint {
        private String channel;
        private long leads;
        private long converted;
        private double conversionRate;
        private BigDecimal revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TreatmentDemandPoint {
        private String treatmentTypeId;
        private String treatmentTypeName;
        private long leadCount;
        private long convertedCount;
        private BigDecimal revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeoDistributionPoint {
        private String country;
        private String countryCode;
        private long leadCount;
        private BigDecimal revenue;
    }
}
