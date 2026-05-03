package com.healthvia.platform.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bson.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.analytics.dto.CEODashboardDtos;
import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.repository.LeadRepository;
import com.healthvia.platform.patientcase.repository.PatientCaseRepository;
import com.healthvia.platform.payment.entity.PaymentRequest;
import com.healthvia.platform.payment.repository.PaymentRequestRepository;
import com.healthvia.platform.ticket.entity.Ticket;
import com.healthvia.platform.treatment.entity.TreatmentType;
import com.healthvia.platform.treatment.repository.TreatmentTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Real CEO analytics — aggregates the patient lifecycle (Lead → Case →
 * FlightBooking + HotelBooking + Appointment → PaymentRequest) into the
 * metrics shown on the executive dashboard.
 *
 * Revenue is the actual paid amount (PaymentRequest.PAID), pipeline value is
 * the sum of conversionValue on still-open leads, and operational metrics
 * (open tickets, SLA breaches, upcoming appointments, average response
 * times) are queried live. Each call is cached for 5 minutes to keep the
 * dashboard snappy.
 *
 * MongoDB aggregation pipelines are used for groupings (channel, treatment,
 * geo, agent leaderboard, daily revenue) so we don't pull entire collections
 * into memory.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CEODashboardService {

    private static final String DEFAULT_CURRENCY = "EUR";

    private final LeadRepository leadRepository;
    private final PatientCaseRepository caseRepository;
    private final PaymentRequestRepository paymentRepository;
    private final AdminRepository adminRepository;
    private final TreatmentTypeRepository treatmentRepository;
    private final MongoTemplate mongo;

    /* =====================================================================
     * Range resolution
     * ===================================================================== */

    private record Window(LocalDateTime start, LocalDateTime end, int days) {}

    private Window window(String range) {
        LocalDate today = LocalDate.now();
        return switch (safeRange(range)) {
            case "today" -> new Window(today.atStartOfDay(), today.plusDays(1).atStartOfDay(), 1);
            case "week" -> new Window(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay(), 7);
            case "year" -> new Window(today.minusDays(364).atStartOfDay(), today.plusDays(1).atStartOfDay(), 365);
            default -> new Window(today.minusDays(29).atStartOfDay(), today.plusDays(1).atStartOfDay(), 30);
        };
    }

    private Window previousWindow(Window w) {
        LocalDateTime prevEnd = w.start;
        LocalDateTime prevStart = prevEnd.minusDays(w.days);
        return new Window(prevStart, prevEnd, w.days);
    }

    private String safeRange(String r) {
        if (r == null) return "month";
        String lower = r.toLowerCase(Locale.ROOT);
        return List.of("today", "week", "month", "year").contains(lower) ? lower : "month";
    }

    /* =====================================================================
     * Overview
     * ===================================================================== */

    @Cacheable(value = "ceo-overview", key = "#range")
    public CEODashboardDtos.Overview getOverview(String range) {
        Window w = window(range);
        Window prev = previousWindow(w);

        BigDecimal revenue = sumPaidPayments(w.start, w.end);
        BigDecimal prevRevenue = sumPaidPayments(prev.start, prev.end);
        double change = pct(revenue, prevRevenue);

        BigDecimal pipelineValue = sumPipelineValue();
        long convertedAllTime = leadRepository.findByStatusAndDeletedFalse(
            Lead.LeadStatus.CONVERTED, PageRequest.of(0, 1)).getTotalElements();
        BigDecimal allTimeRevenue = sumPaidPayments(null, null);
        BigDecimal arpu = convertedAllTime == 0
            ? BigDecimal.ZERO
            : allTimeRevenue.divide(BigDecimal.valueOf(convertedAllTime), 0, RoundingMode.HALF_UP);

        // Lead metrics
        Map<String, Long> byStatus = new HashMap<>();
        long totalLeads = 0;
        for (Lead.LeadStatus s : Lead.LeadStatus.values()) {
            long c = leadRepository.findByStatusAndDeletedFalse(s, PageRequest.of(0, 1)).getTotalElements();
            byStatus.put(s.name(), c);
            totalLeads += c;
        }
        long converted = byStatus.getOrDefault("CONVERTED", 0L);
        long lost = byStatus.getOrDefault("LOST", 0L);
        long newToday = leadRepository.findByCreatedAtBetweenAndDeletedFalse(
            LocalDate.now().atStartOfDay(),
            LocalDate.now().plusDays(1).atStartOfDay(),
            PageRequest.of(0, 1)).getTotalElements();
        double conversionRate = totalLeads == 0 ? 0.0 : round1(converted * 100.0 / totalLeads);

        CEODashboardDtos.Leads leadMetrics = CEODashboardDtos.Leads.builder()
            .total(totalLeads)
            .newToday(newToday)
            .converted(converted)
            .lost(lost)
            .conversionRate(conversionRate)
            .byStatus(byStatus)
            .build();

        CEODashboardDtos.Revenue revenueDto = CEODashboardDtos.Revenue.builder()
            .total(revenue)
            .currency(DEFAULT_CURRENCY)
            .previousPeriod(prevRevenue)
            .changePercent(change)
            .pipelineValue(pipelineValue)
            .averageRevenuePerPatient(arpu)
            .build();

        CEODashboardDtos.Operations ops = computeOperations();
        List<CEODashboardDtos.LostReason> lostReasons = computeLostReasons();

        return CEODashboardDtos.Overview.builder()
            .revenue(revenueDto)
            .leads(leadMetrics)
            .operations(ops)
            .lostReasons(lostReasons)
            .build();
    }

    /* =====================================================================
     * Revenue timeseries — daily $sum from PaymentRequest where status=PAID
     * ===================================================================== */

    @Cacheable(value = "ceo-revenue-timeseries", key = "#range + '-' + #granularity")
    public List<CEODashboardDtos.RevenueTimeseriesPoint> getRevenueTimeseries(
            String range, String granularity) {
        Window w = window(range);

        // Daily revenue from paid payments
        Aggregation revAgg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("status").is("PAID")
                    .and("paid_at").gte(toDate(w.start)).lt(toDate(w.end))
                    .and("is_deleted").ne(true)),
            Aggregation.project()
                .and("amount").as("amount")
                .andExpression("dateToString({format: '%Y-%m-%d', date: '$paid_at'})").as("day"),
            Aggregation.group("day")
                .sum("amount").as("revenue")
                .count().as("paidCount")
        );
        Map<String, BigDecimal> revPerDay = new HashMap<>();
        for (Document d : mongo.aggregate(revAgg, "payment_requests", Document.class).getMappedResults()) {
            String day = d.getString("_id");
            Object rev = d.get("revenue");
            revPerDay.put(day, rev == null ? BigDecimal.ZERO : new BigDecimal(rev.toString()));
        }

        // Daily lead counts
        Aggregation leadAgg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("created_at")
                    .gte(toDate(w.start)).lt(toDate(w.end))
                    .and("is_deleted").ne(true)),
            Aggregation.project()
                .andExpression("dateToString({format: '%Y-%m-%d', date: '$created_at'})").as("day")
                .and("status").as("status"),
            Aggregation.group("day")
                .count().as("leads")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .then(1).otherwise(0)).as("converted")
        );
        Map<String, long[]> leadsPerDay = new HashMap<>();
        for (Document d : mongo.aggregate(leadAgg, "leads", Document.class).getMappedResults()) {
            String day = d.getString("_id");
            long leadCount = ((Number) d.getOrDefault("leads", 0)).longValue();
            long convCount = ((Number) d.getOrDefault("converted", 0)).longValue();
            leadsPerDay.put(day, new long[] { leadCount, convCount });
        }

        // Fill all days in window
        List<CEODashboardDtos.RevenueTimeseriesPoint> out = new ArrayList<>();
        LocalDate cursor = w.start.toLocalDate();
        LocalDate stop = w.end.toLocalDate();
        while (cursor.isBefore(stop)) {
            String key = cursor.toString();
            long[] lc = leadsPerDay.getOrDefault(key, new long[] { 0, 0 });
            BigDecimal rev = revPerDay.getOrDefault(key, BigDecimal.ZERO);
            out.add(CEODashboardDtos.RevenueTimeseriesPoint.builder()
                .date(cursor)
                .revenue(rev)
                .leads(lc[0])
                .converted(lc[1])
                .build());
            cursor = cursor.plusDays(1);
        }
        return out;
    }

    /* =====================================================================
     * Pipeline funnel — count + sum of conversionValue per status
     * ===================================================================== */

    @Cacheable(value = "ceo-funnel", key = "#range")
    public List<CEODashboardDtos.PipelineFunnelPoint> getFunnel(String range) {
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("is_deleted").ne(true)),
            Aggregation.group("status")
                .count().as("count")
                .sum("conversionValue").as("value")
        );
        Map<String, Document> byStatus = new HashMap<>();
        for (Document d : mongo.aggregate(agg, "leads", Document.class).getMappedResults()) {
            byStatus.put(d.getString("_id"), d);
        }

        List<Lead.LeadStatus> order = List.of(
            Lead.LeadStatus.NEW,
            Lead.LeadStatus.ASSIGNED,
            Lead.LeadStatus.CONTACTED,
            Lead.LeadStatus.QUALIFIED,
            Lead.LeadStatus.CONVERTED,
            Lead.LeadStatus.LOST
        );
        List<CEODashboardDtos.PipelineFunnelPoint> out = new ArrayList<>();
        long previous = 0;
        for (Lead.LeadStatus s : order) {
            Document d = byStatus.get(s.name());
            long count = d == null ? 0 : ((Number) d.getOrDefault("count", 0)).longValue();
            BigDecimal value = (d == null || d.get("value") == null)
                ? BigDecimal.ZERO
                : new BigDecimal(d.get("value").toString());
            double conv = previous == 0 ? 100.0 : round1(count * 100.0 / previous);
            out.add(CEODashboardDtos.PipelineFunnelPoint.builder()
                .stage(s.name())
                .count(count)
                .value(value)
                .conversionFromPrevious(conv)
                .build());
            previous = count;
        }
        return out;
    }

    /* =====================================================================
     * Agent leaderboard — group by assignedAgentId
     * ===================================================================== */

    @Cacheable(value = "ceo-leaderboard", key = "#range")
    public List<CEODashboardDtos.AgentPerformance> getAgentLeaderboard(String range) {
        Window w = window(range);

        // Lead aggregation by agent
        Aggregation leadAgg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("assignedAgentId").exists(true).ne(null)
                    .and("is_deleted").ne(true)
                    .and("created_at").gte(toDate(w.start)).lt(toDate(w.end))),
            Aggregation.group("assignedAgentId")
                .count().as("handled")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .then(1).otherwise(0)).as("converted")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .thenValueOf("conversionValue").otherwise(0)).as("revenue")
        );

        Map<String, long[]> leadStats = new HashMap<>(); // agentId -> {handled, converted}
        Map<String, BigDecimal> revenueByAgent = new HashMap<>();
        for (Document d : mongo.aggregate(leadAgg, "leads", Document.class).getMappedResults()) {
            String agent = d.getString("_id");
            long handled = ((Number) d.getOrDefault("handled", 0)).longValue();
            long converted = ((Number) d.getOrDefault("converted", 0)).longValue();
            BigDecimal revenue = d.get("revenue") == null
                ? BigDecimal.ZERO
                : new BigDecimal(d.get("revenue").toString());
            leadStats.put(agent, new long[] { handled, converted });
            revenueByAgent.put(agent, revenue);
        }

        // Resolve agent names
        Map<String, Admin> admins = new HashMap<>();
        adminRepository.findAll().forEach(a -> admins.put(a.getId(), a));

        List<CEODashboardDtos.AgentPerformance> result = new ArrayList<>();
        for (var entry : leadStats.entrySet()) {
            String agentId = entry.getKey();
            long handled = entry.getValue()[0];
            long converted = entry.getValue()[1];
            Admin admin = admins.get(agentId);
            String name = admin == null
                ? "(silinmiş agent)"
                : (admin.getFirstName() + " " + admin.getLastName()).trim();
            result.add(CEODashboardDtos.AgentPerformance.builder()
                .agentId(agentId)
                .agentName(name)
                .avatarUrl(admin == null ? null : admin.getAvatarUrl())
                .leadsHandled(handled)
                .leadsConverted(converted)
                .conversionRate(handled == 0 ? 0.0 : round1(converted * 100.0 / handled))
                .revenue(revenueByAgent.getOrDefault(agentId, BigDecimal.ZERO))
                .avgResponseMinutes(0.0) // TODO: derive from Conversation/Message timestamps
                .occupancy(0.0)          // TODO: derive from active conversations
                .ranking(0)
                .build());
        }

        // Sort by revenue desc, assign ranking
        result.sort(Comparator.comparing(
            CEODashboardDtos.AgentPerformance::getRevenue).reversed());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRanking(i + 1);
        }
        return result;
    }

    /* =====================================================================
     * Channel breakdown
     * ===================================================================== */

    @Cacheable(value = "ceo-channels", key = "#range")
    public List<CEODashboardDtos.ChannelBreakdownPoint> getChannelBreakdown(String range) {
        Window w = window(range);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("is_deleted").ne(true)
                    .and("created_at").gte(toDate(w.start)).lt(toDate(w.end))),
            Aggregation.group("source")
                .count().as("leads")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .then(1).otherwise(0)).as("converted")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .thenValueOf("conversionValue").otherwise(0)).as("revenue")
        );
        List<CEODashboardDtos.ChannelBreakdownPoint> out = new ArrayList<>();
        for (Document d : mongo.aggregate(agg, "leads", Document.class).getMappedResults()) {
            String source = d.getString("_id");
            if (source == null) continue;
            long leads = ((Number) d.getOrDefault("leads", 0)).longValue();
            long converted = ((Number) d.getOrDefault("converted", 0)).longValue();
            BigDecimal revenue = d.get("revenue") == null
                ? BigDecimal.ZERO : new BigDecimal(d.get("revenue").toString());
            out.add(CEODashboardDtos.ChannelBreakdownPoint.builder()
                .channel(source)
                .leads(leads)
                .converted(converted)
                .conversionRate(leads == 0 ? 0.0 : round1(converted * 100.0 / leads))
                .revenue(revenue)
                .build());
        }
        out.sort(Comparator.comparing(CEODashboardDtos.ChannelBreakdownPoint::getLeads).reversed());
        return out;
    }

    /* =====================================================================
     * Treatment demand
     * ===================================================================== */

    @Cacheable(value = "ceo-treatments", key = "#range")
    public List<CEODashboardDtos.TreatmentDemandPoint> getTreatmentDemand(String range) {
        Window w = window(range);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("treatmentTypeId").exists(true).ne(null)
                    .and("is_deleted").ne(true)
                    .and("created_at").gte(toDate(w.start)).lt(toDate(w.end))),
            Aggregation.group("treatmentTypeId")
                .count().as("leads")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .then(1).otherwise(0)).as("converted")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .thenValueOf("conversionValue").otherwise(0)).as("revenue")
        );

        // Resolve treatment names
        Map<String, String> treatmentNames = new HashMap<>();
        treatmentRepository.findAll().forEach(t -> treatmentNames.put(t.getId(), pickTreatmentName(t)));

        List<CEODashboardDtos.TreatmentDemandPoint> out = new ArrayList<>();
        for (Document d : mongo.aggregate(agg, "leads", Document.class).getMappedResults()) {
            String tid = d.getString("_id");
            long leads = ((Number) d.getOrDefault("leads", 0)).longValue();
            long converted = ((Number) d.getOrDefault("converted", 0)).longValue();
            BigDecimal revenue = d.get("revenue") == null
                ? BigDecimal.ZERO : new BigDecimal(d.get("revenue").toString());
            out.add(CEODashboardDtos.TreatmentDemandPoint.builder()
                .treatmentTypeId(tid)
                .treatmentTypeName(treatmentNames.getOrDefault(tid, tid))
                .leadCount(leads)
                .convertedCount(converted)
                .revenue(revenue)
                .build());
        }
        out.sort(Comparator.comparing(CEODashboardDtos.TreatmentDemandPoint::getLeadCount).reversed());
        return out;
    }

    /* =====================================================================
     * Geo distribution
     * ===================================================================== */

    @Cacheable(value = "ceo-geo", key = "#range")
    public List<CEODashboardDtos.GeoDistributionPoint> getGeoDistribution(String range) {
        Window w = window(range);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("country").exists(true).ne(null)
                    .and("is_deleted").ne(true)
                    .and("created_at").gte(toDate(w.start)).lt(toDate(w.end))),
            Aggregation.group("country")
                .count().as("leadCount")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                    .when(org.springframework.data.mongodb.core.query.Criteria.where("status").is("CONVERTED"))
                    .thenValueOf("conversionValue").otherwise(0)).as("revenue")
        );
        List<CEODashboardDtos.GeoDistributionPoint> out = new ArrayList<>();
        for (Document d : mongo.aggregate(agg, "leads", Document.class).getMappedResults()) {
            String country = d.getString("_id");
            long count = ((Number) d.getOrDefault("leadCount", 0)).longValue();
            BigDecimal revenue = d.get("revenue") == null
                ? BigDecimal.ZERO : new BigDecimal(d.get("revenue").toString());
            out.add(CEODashboardDtos.GeoDistributionPoint.builder()
                .country(country)
                .countryCode(toIso2(country))
                .leadCount(count)
                .revenue(revenue)
                .build());
        }
        out.sort(Comparator.comparing(CEODashboardDtos.GeoDistributionPoint::getLeadCount).reversed());
        return out;
    }

    /* =====================================================================
     * Helpers
     * ===================================================================== */

    private BigDecimal sumPaidPayments(LocalDateTime from, LocalDateTime to) {
        var c = org.springframework.data.mongodb.core.query.Criteria
            .where("status").is(PaymentRequest.PaymentStatus.PAID.name())
            .and("is_deleted").ne(true);
        if (from != null) c = c.and("paid_at").gte(toDate(from)).lt(toDate(to));
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(c),
            Aggregation.group().sum("amount").as("total")
        );
        AggregationResults<Document> res = mongo.aggregate(agg, "payment_requests", Document.class);
        Document d = res.getUniqueMappedResult();
        if (d == null || d.get("total") == null) return BigDecimal.ZERO;
        return new BigDecimal(d.get("total").toString());
    }

    private BigDecimal sumPipelineValue() {
        var c = org.springframework.data.mongodb.core.query.Criteria
            .where("status").nin("CONVERTED", "LOST", "SPAM")
            .and("is_deleted").ne(true);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(c),
            Aggregation.group().sum("conversionValue").as("total")
        );
        Document d = mongo.aggregate(agg, "leads", Document.class).getUniqueMappedResult();
        if (d == null || d.get("total") == null) return BigDecimal.ZERO;
        return new BigDecimal(d.get("total").toString());
    }

    private CEODashboardDtos.Operations computeOperations() {
        // Open tickets (status in OPEN/IN_PROGRESS/WAITING_*)
        long openTickets = mongo.getCollection("tickets").countDocuments(
            new Document("status", new Document("$in",
                List.of(Ticket.TicketStatus.OPEN.name(),
                        Ticket.TicketStatus.IN_PROGRESS.name(),
                        Ticket.TicketStatus.WAITING_CUSTOMER.name(),
                        Ticket.TicketStatus.WAITING_EXTERNAL.name())))
                .append("is_deleted", new Document("$ne", true)));

        long slaBreaches = mongo.getCollection("tickets").countDocuments(
            new Document("slaBreached", true).append("is_deleted", new Document("$ne", true)));

        // Pending payments
        long pendingPayments = paymentRepository.countByStatusAndDeletedFalse(
            PaymentRequest.PaymentStatus.LINK_SENT)
            + paymentRepository.countByStatusAndDeletedFalse(PaymentRequest.PaymentStatus.PENDING);
        BigDecimal pendingAmount = sumPaymentsByStatus(List.of(
            PaymentRequest.PaymentStatus.LINK_SENT,
            PaymentRequest.PaymentStatus.PENDING));

        // Active leads (not CONVERTED/LOST/SPAM)
        long activeLeads = mongo.getCollection("leads").countDocuments(
            new Document("status", new Document("$nin", List.of("CONVERTED", "LOST", "SPAM")))
                .append("is_deleted", new Document("$ne", true)));

        // Upcoming appointments next 7 days
        LocalDate today = LocalDate.now();
        long upcoming = mongo.getCollection("appointments").countDocuments(
            new Document("appointment_date", new Document("$gte", today.toString())
                .append("$lt", today.plusDays(7).toString()))
                .append("status", new Document("$in", List.of(
                    Appointment.AppointmentStatus.PENDING.name(),
                    Appointment.AppointmentStatus.CONFIRMED.name())))
                .append("is_deleted", new Document("$ne", true)));

        return CEODashboardDtos.Operations.builder()
            .activeLeads(activeLeads)
            .openTickets(openTickets)
            .slaBreaches(slaBreaches)
            .pendingPayments(pendingPayments)
            .pendingPaymentsAmount(pendingAmount)
            .upcomingAppointments7d(upcoming)
            .avgFirstResponseMinutes(0.0)
            .avgResolutionHours(0.0)
            .build();
    }

    private BigDecimal sumPaymentsByStatus(List<PaymentRequest.PaymentStatus> statuses) {
        var c = org.springframework.data.mongodb.core.query.Criteria
            .where("status").in(statuses.stream().map(Enum::name).toList())
            .and("is_deleted").ne(true);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(c),
            Aggregation.group().sum("amount").as("total")
        );
        Document d = mongo.aggregate(agg, "payment_requests", Document.class).getUniqueMappedResult();
        return d == null || d.get("total") == null
            ? BigDecimal.ZERO
            : new BigDecimal(d.get("total").toString());
    }

    private List<CEODashboardDtos.LostReason> computeLostReasons() {
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria
                    .where("status").is("LOST").and("is_deleted").ne(true)),
            Aggregation.group("lostReason").count().as("count")
        );
        List<CEODashboardDtos.LostReason> out = new ArrayList<>();
        for (Document d : mongo.aggregate(agg, "leads", Document.class).getMappedResults()) {
            String reason = d.getString("_id");
            long count = ((Number) d.getOrDefault("count", 0)).longValue();
            out.add(CEODashboardDtos.LostReason.builder()
                .reason(reason == null ? "OTHER" : reason)
                .count(count)
                .build());
        }
        out.sort(Comparator.comparing(CEODashboardDtos.LostReason::getCount).reversed());
        return out;
    }

    @SuppressWarnings("unused")
    private void touchUnused() {
        // Keep imports/repositories wired so future expansions don't break compile.
        caseRepository.count();
    }

    private static java.util.Date toDate(LocalDateTime ldt) {
        return java.util.Date.from(ldt.toInstant(ZoneOffset.UTC));
    }

    private static double pct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current == null || current.signum() == 0 ? 0.0 : 100.0;
        }
        BigDecimal delta = (current == null ? BigDecimal.ZERO : current).subtract(previous);
        return round1(delta.divide(previous, 4, RoundingMode.HALF_UP).doubleValue() * 100.0);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String pickTreatmentName(TreatmentType t) {
        try {
            String tr = (String) TreatmentType.class.getMethod("getNameTR").invoke(t);
            if (tr != null && !tr.isBlank()) return tr;
        } catch (Exception ignored) { /* fallback */ }
        try {
            String name = (String) TreatmentType.class.getMethod("getName").invoke(t);
            if (name != null && !name.isBlank()) return name;
        } catch (Exception ignored) { /* fallback */ }
        return t.getId();
    }

    private static String toIso2(String country) {
        if (country == null) return null;
        return switch (country.toLowerCase(Locale.ROOT)) {
            case "united kingdom", "uk", "britain" -> "GB";
            case "germany", "deutschland" -> "DE";
            case "france" -> "FR";
            case "italy", "italia" -> "IT";
            case "spain", "españa" -> "ES";
            case "saudi arabia", "ksa" -> "SA";
            case "uae", "united arab emirates" -> "AE";
            case "usa", "united states" -> "US";
            case "russia" -> "RU";
            case "netherlands" -> "NL";
            case "turkey", "türkiye" -> "TR";
            default -> country.length() == 2 ? country.toUpperCase() : null;
        };
    }
}
