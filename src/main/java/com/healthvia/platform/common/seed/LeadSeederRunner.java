package com.healthvia.platform.common.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.AssignmentMethod;
import com.healthvia.platform.lead.entity.Lead.LeadPriority;
import com.healthvia.platform.lead.entity.Lead.LeadSource;
import com.healthvia.platform.lead.entity.Lead.LeadStatus;
import com.healthvia.platform.lead.repository.LeadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo Lead seeder — populates a varied pipeline so the CEO dashboard is
 * not empty on a fresh database. Activated by:
 *
 *   app.seed.demo-leads=true
 *
 * Idempotent: skips seeding if any leads already exist.
 *
 * The 30-row seed covers:
 *   - 8 distinct sources (WhatsApp / Instagram / Email / Phone / Web form
 *     / Google Ads / Referral / Partner)
 *   - 10 origin countries spanning EU, MENA, Russia, Pakistan, USA
 *   - All 12 LeadStatus values, weighted toward an active pipeline
 *   - Realistic conversionValue + currency for converted leads (drives
 *     revenue / pipeline / leaderboard / ARPU on the CEO dashboard)
 */
@Slf4j
@Component
@Order(50) // run after Mongo connection is ready
@RequiredArgsConstructor
public class LeadSeederRunner implements CommandLineRunner {

    private final LeadRepository leadRepository;
    private final AdminRepository adminRepository;

    @Value("${app.seed.demo-leads:false}")
    private boolean enabled;

    private static final Random R = new Random(20260417L);

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.debug("Lead seeding disabled (app.seed.demo-leads=false)");
            return;
        }
        long existing = leadRepository.count();
        if (existing > 0) {
            log.info("Leads already present ({}), skipping seed.", existing);
            return;
        }

        // Load real seeded HealthVia AGENTs so we can assign them via DB ids,
        // not random placeholder strings. If team seeder hasn't run, we just
        // leave assignedAgentId null — autoAssign'll pick later when an agent
        // logs in.
        List<String> realAgentIds = adminRepository.findAll().stream()
            .filter(a -> a.getRole() == UserRole.AGENT)
            .filter(a -> !a.isDeleted())
            .map(Admin::getId)
            .toList();
        log.info("🌱 Seeding {} demo leads (agentPool size={})...", SEED_DATA.size(), realAgentIds.size());

        int saved = 0;
        for (int i = 0; i < SEED_DATA.size(); i++) {
            LeadSeed s = SEED_DATA.get(i);
            try {
                Lead lead = s.toEntity(realAgentIds, i);
                leadRepository.save(lead);
                saved += 1;
            } catch (Exception e) {
                log.warn("Lead seed insert failed: {} — {}", s.firstName, e.getMessage());
            }
        }
        log.info("✅ Seeded {} demo leads.", saved);
    }

    /* ---------- Data ---------- */

    private record LeadSeed(
        String firstName, String lastName,
        String email, String phone,
        String country, String language,
        LeadSource source, LeadStatus status, LeadPriority priority,
        String treatmentTypeId,
        BigDecimal conversionValue,
        Set<String> tags,
        int daysAgo) {

        Lead toEntity(List<String> agentPool, int index) {
            LocalDateTime created = LocalDateTime.now().minusDays(daysAgo).minusHours(R.nextInt(24));
            LocalDateTime updated = created.plusHours(2 + R.nextInt(48));
            // Round-robin across real seeded agents; fall back to null if pool is empty.
            String assignedAgent = (status == LeadStatus.NEW || agentPool.isEmpty())
                ? null
                : agentPool.get(index % agentPool.size());
            return Lead.builder()
                .firstName(firstName).lastName(lastName)
                .email(email).phone(phone)
                .country(country).language(language)
                .source(source)
                .status(status)
                .priority(priority)
                .treatmentTypeId(treatmentTypeId)
                .conversionValue(conversionValue)
                .convertedAt(status == LeadStatus.CONVERTED ? updated : null)
                .assignedAgentId(assignedAgent)
                .assignmentMethod(assignedAgent == null ? null : AssignmentMethod.AUTO)
                .previousAgentIds(new ArrayList<>())
                .tags(tags)
                .createdAt(created)
                .updatedAt(updated)
                .build();
        }
    }

    @SuppressWarnings("unused")
    private static Optional<String> firstId(List<String> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private static final List<LeadSeed> SEED_DATA = List.of(
        // CONVERTED — drive revenue
        new LeadSeed("George", "Anderson", "george.anderson@example.com", "+447700900030",
            "United Kingdom", "EN", LeadSource.REFERRAL, LeadStatus.CONVERTED, LeadPriority.HIGH,
            "tt_hair", new BigDecimal("2800"), Set.of("converted","referral"), 18),
        new LeadSeed("Hanna", "Schmidt", "hanna.schmidt@example.de", "+491701234013",
            "Germany", "DE", LeadSource.REFERRAL, LeadStatus.CONVERTED, LeadPriority.MEDIUM,
            "tt_ivf", new BigDecimal("6800"), Set.of("converted"), 32),
        new LeadSeed("Youssef", "Mansour", "youssef.mansour@example.eg", "+201001234012",
            "Egypt", "AR", LeadSource.WHATSAPP, LeadStatus.CONVERTED, LeadPriority.HIGH,
            "tt_dental", new BigDecimal("4200"), Set.of("converted"), 28),
        new LeadSeed("Dmitri", "Sokolov", "dmitri@example.ru", "+79161234024",
            "Russia", "RU", LeadSource.WEB_FORM, LeadStatus.CONVERTED, LeadPriority.HIGH,
            "tt_bbl", new BigDecimal("5400"), Set.of("converted","aobo"), 19),
        new LeadSeed("Ahmed", "Al-Rashid", "ahmed.r@example.sa", "+966501230009",
            "Saudi Arabia", "AR", LeadSource.WHATSAPP, LeadStatus.CONVERTED, LeadPriority.URGENT,
            "tt_cardio", new BigDecimal("18500"), Set.of("converted","vip"), 12),
        new LeadSeed("Linda", "Jackson", "linda.jackson@example.com", "+14155550011",
            "United States", "EN", LeadSource.GOOGLE_ADS, LeadStatus.CONVERTED, LeadPriority.HIGH,
            "tt_rhino", new BigDecimal("4500"), Set.of("converted"), 7),

        // QUALIFIED / PROPOSAL_SENT — pipeline value
        new LeadSeed("Lucia", "Bianchi", "lucia.bianchi@example.it", "+390612345003",
            "Italy", "IT", LeadSource.EMAIL, LeadStatus.PROPOSAL_SENT, LeadPriority.HIGH,
            "tt_ivf", new BigDecimal("6800"), Set.of("clinic-tour"), 3),
        new LeadSeed("Peter", "Mueller", "peter.mueller@example.de", "+491701234004",
            "Germany", "DE", LeadSource.GOOGLE_ADS, LeadStatus.QUALIFIED, LeadPriority.HIGH,
            "tt_bariatric", new BigDecimal("5500"), Set.of("bmi-38"), 5),
        new LeadSeed("Emma", "Taylor", "emma.taylor@example.com", "+14155550019",
            "United States", "EN", LeadSource.REFERRAL, LeadStatus.QUALIFIED, LeadPriority.HIGH,
            "tt_dental", new BigDecimal("12000"), Set.of("all-on-4","referral"), 4),
        new LeadSeed("Felix", "Wagner", "felix.wagner@example.de", "+491701234028",
            "Germany", "DE", LeadSource.WHATSAPP, LeadStatus.QUALIFIED, LeadPriority.HIGH,
            "tt_ortho", new BigDecimal("8000"), Set.of("arthroscopy"), 6),
        new LeadSeed("Tariq", "Hamid", "tariq.hamid@example.sa", "+96611234025",
            "Saudi Arabia", "AR", LeadSource.WHATSAPP, LeadStatus.QUALIFIED, LeadPriority.URGENT,
            "tt_onco", new BigDecimal("28000"), Set.of("second-opinion","urgent"), 1),

        // CONTACTED / NEGOTIATION — early funnel
        new LeadSeed("Fatma", "Khalil", null, "+971501234002",
            "United Arab Emirates", "AR", LeadSource.WHATSAPP, LeadStatus.CONTACTED, LeadPriority.HIGH,
            "tt_bbl", new BigDecimal("5200"), Set.of("package-interest"), 2),
        new LeadSeed("Rashid", "Khan", null, "+923001234006",
            "Pakistan", "EN", LeadSource.PHONE, LeadStatus.CONTACTED, LeadPriority.URGENT,
            "tt_cardio", new BigDecimal("15000"), Set.of("urgent-medical","cardiac"), 1),
        new LeadSeed("Ivan", "Petrov", null, "+79161234008",
            "Russia", "RU", LeadSource.WHATSAPP, LeadStatus.CONTACTED, LeadPriority.MEDIUM,
            "tt_hair", new BigDecimal("3200"), Set.of("peak-season"), 3),
        new LeadSeed("Clara", "Fernandez", "clara.fernandez@example.es", "+34612345021",
            "Spain", "ES", LeadSource.WEB_FORM, LeadStatus.NEGOTIATION, LeadPriority.MEDIUM,
            "tt_bariatric", new BigDecimal("4500"), Set.of(), 4),

        // ASSIGNED — fresh
        new LeadSeed("John", "Smith", "john.smith@example.com", "+447700900001",
            "United Kingdom", "EN", LeadSource.WEB_FORM, LeadStatus.ASSIGNED, LeadPriority.MEDIUM,
            "tt_dental", new BigDecimal("0"), Set.of("full-mouth"), 1),
        new LeadSeed("Sophie", "Laurent", "sophie.laurent@example.fr", "+33612345007",
            "France", "FR", LeadSource.INSTAGRAM, LeadStatus.ASSIGNED, LeadPriority.MEDIUM,
            "tt_veneer", new BigDecimal("0"), Set.of("hollywood-smile"), 0),
        new LeadSeed("Natalia", "Ivanova", null, "+79161234018",
            "Russia", "RU", LeadSource.GOOGLE_ADS, LeadStatus.ASSIGNED, LeadPriority.MEDIUM,
            "tt_ortho", new BigDecimal("0"), Set.of("knee-replacement"), 0),
        new LeadSeed("Mohammed", "Al-Rashid", null, "+96891234020",
            "Oman", "AR", LeadSource.PHONE, LeadStatus.ASSIGNED, LeadPriority.HIGH,
            "tt_cardio", new BigDecimal("0"), Set.of("vip"), 0),
        new LeadSeed("Aisha", "Bello", null, "+234801234023",
            "Nigeria", "EN", LeadSource.WHATSAPP, LeadStatus.ASSIGNED, LeadPriority.HIGH,
            "tt_ivf", new BigDecimal("0"), Set.of("fertility"), 1),
        new LeadSeed("Nora", "Olsen", "nora.olsen@example.no", "+4791234026",
            "Norway", "EN", LeadSource.PARTNER, LeadStatus.ASSIGNED, LeadPriority.MEDIUM,
            "tt_lasik", new BigDecimal("0"), Set.of(), 0),

        // NEW — top of funnel
        new LeadSeed("Maria", "Gonzalez", "maria.gonzalez@example.es", "+34612345001",
            "Spain", "ES", LeadSource.WHATSAPP, LeadStatus.NEW, LeadPriority.HIGH,
            "tt_hair", new BigDecimal("0"), Set.of("hot-lead","budget-approved"), 0),
        new LeadSeed("Ahmed", "Al-Farouk", "ahmed.farouk@example.sa", "+966501234001",
            "Saudi Arabia", "AR", LeadSource.INSTAGRAM, LeadStatus.NEW, LeadPriority.URGENT,
            "tt_rhino", new BigDecimal("0"), Set.of("revision-case","premium"), 0),
        new LeadSeed("Olivia", "Brown", "olivia.brown@example.com", "+61412345005",
            "Australia", "EN", LeadSource.WEB_FORM, LeadStatus.NEW, LeadPriority.MEDIUM,
            "tt_lasik", new BigDecimal("0"), Set.of("tourism-combined"), 0),
        new LeadSeed("Carlos", "Ruiz", "carlos.ruiz@example.mx", "+521551234015",
            "Mexico", "ES", LeadSource.INSTAGRAM, LeadStatus.NEW, LeadPriority.MEDIUM,
            "tt_hair", new BigDecimal("0"), Set.of(), 0),
        new LeadSeed("Beatrice", "Moretti", "beatrice.moretti@example.it", "+390612345029",
            "Italy", "IT", LeadSource.INSTAGRAM, LeadStatus.NEW, LeadPriority.MEDIUM,
            "tt_hair", new BigDecimal("0"), Set.of(), 0),
        new LeadSeed("Viktor", "Kovac", null, "+385981234022",
            "Croatia", "EN", LeadSource.INSTAGRAM, LeadStatus.NEW, LeadPriority.MEDIUM,
            "tt_hair", new BigDecimal("0"), Set.of(), 1),

        // LOST — for funnel completeness + lost-reason chart
        new LeadSeed("Amina", "Toure", null, "+221771234014",
            "Senegal", "FR", LeadSource.EMAIL, LeadStatus.LOST, LeadPriority.LOW,
            "tt_hair", new BigDecimal("0"), Set.of("budget-rejected"), 14),
        new LeadSeed("Khadija", "Benali", null, "+21251234027",
            "Morocco", "FR", LeadSource.EMAIL, LeadStatus.LOST, LeadPriority.LOW,
            "tt_rhino", new BigDecimal("0"), Set.of(), 21),
        new LeadSeed("Anja", "Novak", null, "+386412345016",
            "Slovenia", "EN", LeadSource.WEB_FORM, LeadStatus.LOST, LeadPriority.LOW,
            "tt_veneer", new BigDecimal("0"), Set.of("low-budget"), 9),

        // SPAM
        new LeadSeed("Bot", "User", "spam@example.cn", "+8612345600001",
            "China", "EN", LeadSource.WEB_FORM, LeadStatus.SPAM, LeadPriority.LOW,
            null, new BigDecimal("0"), Set.of("spam"), 5)
    );

    @SuppressWarnings("unused")
    private static LocalDate today() { return LocalDate.now(); }
}
