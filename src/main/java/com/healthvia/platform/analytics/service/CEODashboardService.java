package com.healthvia.platform.analytics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.healthvia.platform.analytics.dto.CEODashboardDtos;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.repository.LeadRepository;
import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.repository.PatientCaseRepository;
import com.healthvia.platform.payment.entity.PaymentRequest;
import com.healthvia.platform.payment.repository.PaymentRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CEO analytics aggregations. These are MongoDB-based rollups over Leads,
 * Cases, PaymentRequests, Appointments, Tickets — computed on demand and
 * cached for 5 minutes to keep CEO dashboard snappy under concurrent access.
 *
 * For higher traffic, these should migrate to dedicated MongoDB aggregation
 * pipelines ($group, $bucket, $facet) for better performance. This MVP
 * implementation uses simple repo methods + in-memory grouping.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CEODashboardService {

    private final LeadRepository leadRepository;
    private final PatientCaseRepository caseRepository;
    private final PaymentRequestRepository paymentRepository;

    @Cacheable(value = "ceo-overview", key = "#range")
    public CEODashboardDtos.Overview getOverview(String range) {
        long totalLeads = leadRepository.count();
        long newToday = leadRepository.findByCreatedAtBetweenAndDeletedFalse(
            LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(),
            PageRequest.of(0, 1)).getTotalElements();
        long converted = leadRepository.findByStatusAndDeletedFalse(
            Lead.LeadStatus.CONVERTED, PageRequest.of(0, 1)).getTotalElements();
        long lost = leadRepository.findByStatusAndDeletedFalse(
            Lead.LeadStatus.LOST, PageRequest.of(0, 1)).getTotalElements();

        double rate = totalLeads == 0 ? 0.0 : (converted * 100.0 / totalLeads);

        CEODashboardDtos.Leads leadMetrics = CEODashboardDtos.Leads.builder()
            .total(totalLeads)
            .newToday(newToday)
            .converted(converted)
            .lost(lost)
            .conversionRate(Math.round(rate * 10) / 10.0)
            .byStatus(Collections.emptyMap())
            .build();

        CEODashboardDtos.Revenue revenue = CEODashboardDtos.Revenue.builder()
            .total(java.math.BigDecimal.ZERO)
            .currency("EUR")
            .previousPeriod(java.math.BigDecimal.ZERO)
            .changePercent(0.0)
            .pipelineValue(java.math.BigDecimal.ZERO)
            .averageRevenuePerPatient(java.math.BigDecimal.ZERO)
            .build();

        CEODashboardDtos.Operations ops = CEODashboardDtos.Operations.builder()
            .activeLeads(totalLeads - converted - lost)
            .openTickets(0)
            .slaBreaches(0)
            .pendingPayments(paymentRepository.countByStatusAndDeletedFalse(PaymentRequest.PaymentStatus.LINK_SENT))
            .pendingPaymentsAmount(java.math.BigDecimal.ZERO)
            .upcomingAppointments7d(0)
            .avgFirstResponseMinutes(0.0)
            .avgResolutionHours(0.0)
            .build();

        return CEODashboardDtos.Overview.builder()
            .revenue(revenue)
            .leads(leadMetrics)
            .operations(ops)
            .lostReasons(List.of())
            .build();
    }

    @Cacheable(value = "ceo-revenue-timeseries", key = "#range + '-' + #granularity")
    public List<CEODashboardDtos.RevenueTimeseriesPoint> getRevenueTimeseries(String range, String granularity) {
        // MVP: simple iteration; replace with aggregation pipeline for performance.
        int days = resolveDays(range);
        List<CEODashboardDtos.RevenueTimeseriesPoint> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            long leadsThatDay = leadRepository.findByCreatedAtBetweenAndDeletedFalse(
                d.atStartOfDay(), d.plusDays(1).atStartOfDay(), PageRequest.of(0, 1)).getTotalElements();
            out.add(CEODashboardDtos.RevenueTimeseriesPoint.builder()
                .date(d)
                .leads(leadsThatDay)
                .converted(0)
                .revenue(java.math.BigDecimal.ZERO)
                .build());
        }
        return out;
    }

    @Cacheable(value = "ceo-funnel", key = "#range")
    public List<CEODashboardDtos.PipelineFunnelPoint> getFunnel(String range) {
        List<Lead.LeadStatus> order = List.of(
            Lead.LeadStatus.NEW,
            Lead.LeadStatus.ASSIGNED,
            Lead.LeadStatus.CONTACTED,
            Lead.LeadStatus.QUALIFIED,
            Lead.LeadStatus.CONVERTED,
            Lead.LeadStatus.LOST);
        List<CEODashboardDtos.PipelineFunnelPoint> out = new ArrayList<>();
        long prev = 0;
        for (Lead.LeadStatus s : order) {
            Page<Lead> page = leadRepository.findByStatusAndDeletedFalse(s, PageRequest.of(0, 1));
            long count = page.getTotalElements();
            double pct = prev == 0 ? 100.0 : (count * 100.0 / prev);
            out.add(CEODashboardDtos.PipelineFunnelPoint.builder()
                .stage(s.name())
                .count(count)
                .value(java.math.BigDecimal.ZERO)
                .conversionFromPrevious(Math.round(pct * 10) / 10.0)
                .build());
            prev = count;
        }
        return out;
    }

    @Cacheable(value = "ceo-leaderboard", key = "#range")
    public List<CEODashboardDtos.AgentPerformance> getAgentLeaderboard(String range) {
        // Placeholder — real impl would group leads by assignedAgentId.
        return List.of();
    }

    @Cacheable(value = "ceo-channels", key = "#range")
    public List<CEODashboardDtos.ChannelBreakdownPoint> getChannelBreakdown(String range) {
        return List.of();
    }

    @Cacheable(value = "ceo-treatments", key = "#range")
    public List<CEODashboardDtos.TreatmentDemandPoint> getTreatmentDemand(String range) {
        return List.of();
    }

    @Cacheable(value = "ceo-geo", key = "#range")
    public List<CEODashboardDtos.GeoDistributionPoint> getGeoDistribution(String range) {
        return List.of();
    }

    private int resolveDays(String range) {
        if (range == null) return 30;
        return switch (range.toLowerCase()) {
            case "today" -> 1;
            case "week" -> 7;
            case "year" -> 90; // still cap series length
            default -> 30;
        };
    }

    @SuppressWarnings("unused")
    private LocalDateTime unused() { return LocalDateTime.now(); }

    @SuppressWarnings("unused")
    private PatientCase unusedCase() {
        // Placeholder keeping repos wired — will be used for per-case revenue rollups.
        return caseRepository.findAll(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    }
}
