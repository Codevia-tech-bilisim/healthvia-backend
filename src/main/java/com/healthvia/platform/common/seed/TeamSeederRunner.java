package com.healthvia.platform.common.seed;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.entity.Admin.AdminLevel;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.common.enums.Language;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo HealthVia team seeder — populates Admin (employee) records so the
 * inbox / dashboard / auto-assignment have real users to operate on.
 *
 * Activated by: app.seed.demo-team=true
 *
 * Idempotent per email: each account is only inserted if its email does not
 * already exist. Running on every restart is safe.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class TeamSeederRunner implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-team:false}")
    private boolean enabled;

    @Value("${app.seed.demo-password:demo123}")
    private String demoPassword;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.debug("Team seeding disabled (app.seed.demo-team=false)");
            return;
        }

        String pw = passwordEncoder.encode(demoPassword);
        int saved = 0;
        int skipped = 0;

        for (TeamMember m : TEAM_SEED) {
            if (adminRepository.existsByEmail(m.email())) {
                skipped++;
                continue;
            }
            try {
                adminRepository.save(m.toEntity(pw));
                saved++;
                log.info("  ✓ Seeded {} ({})", m.email(), m.role());
            } catch (Exception e) {
                log.warn("Team seed insert failed: {} — {}: {}",
                        m.email(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        if (saved > 0) {
            log.info("✅ Seeded {} new HealthVia accounts (skipped {}, default password: {}).",
                    saved, skipped, demoPassword);
        } else {
            log.info("✅ All {} HealthVia accounts already present, nothing to seed.", skipped);
        }
    }

    /* ---------- Data ---------- */

    private record TeamMember(
            String email, String phone, String firstName, String lastName,
            UserRole role, AdminLevel level, String department, String jobTitle,
            Set<String> languages, Set<String> specializations,
            String avatarUrl) {

        Admin toEntity(String passwordHash) {
            Admin a = Admin.builder()
                    .email(email)
                    .phone(phone)
                    .password(passwordHash)
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .phoneVerified(false)
                    .gdprConsent(true)
                    .gdprConsentDate(LocalDateTime.now())
                    .failedLoginAttempts(0)
                    .profileCompletionRate(80)
                    .preferredLanguage(toLanguage(languages.iterator().next()))
                    .avatarUrl(avatarUrl)
                    .department(department)
                    .jobTitle(jobTitle)
                    .adminLevel(level)
                    .employeeId("HV-" + (1000 + Math.abs(email.hashCode() % 9000)))
                    .hireDate(LocalDateTime.now())
                    .canManageUsers(level == AdminLevel.SUPER_ADMIN || level == AdminLevel.MANAGER)
                    .canManageDoctors(level == AdminLevel.SUPER_ADMIN || level == AdminLevel.MANAGER)
                    .canManageClinics(level == AdminLevel.SUPER_ADMIN)
                    .canViewReports(true)
                    .canManageSystem(level == AdminLevel.SUPER_ADMIN)
                    .spokenLanguages(new HashSet<>(languages))
                    .specializations(new HashSet<>(specializations))
                    .isAvailable(true)
                    .maxConcurrentChats(8)
                    .currentActiveChats(0)
                    .assignedLeadCount(0)
                    .convertedLeadCount(0)
                    .totalActionsPerformed(0)
                    .usersManaged(0)
                    .doctorsApproved(0)
                    .clinicsApproved(0)
                    .workingDays(new HashSet<>(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")))
                    .build();
            return a;
        }
    }

    private static final List<TeamMember> TEAM_SEED = List.of(
            new TeamMember(
                    "root@healthvia.com", "+905550000001", "Sistem", "Yöneticisi",
                    UserRole.SUPERADMIN, AdminLevel.SUPER_ADMIN,
                    "Sistem", "Süper Yönetici",
                    Set.of("TR", "EN"), Set.of(),
                    "https://i.pravatar.cc/150?img=68"),

            new TeamMember(
                    "sinem.kara@healthvia.com", "+905550000002", "Sinem", "Kara",
                    UserRole.CEO, AdminLevel.MANAGER,
                    "Yönetim", "CEO",
                    Set.of("TR", "EN", "FR"), Set.of(),
                    "https://i.pravatar.cc/150?img=48"),

            new TeamMember(
                    "mert.yilmaz@healthvia.com", "+905550000003", "Mert", "Yılmaz",
                    UserRole.ADMIN, AdminLevel.MANAGER,
                    "Operasyon", "Operasyon Müdürü (Lead Yöneticisi)",
                    Set.of("TR", "EN"), Set.of(),
                    "https://i.pravatar.cc/150?img=14"),

            new TeamMember(
                    "zeynep.aydin@healthvia.com", "+905550000004", "Zeynep", "Aydın",
                    UserRole.AGENT, AdminLevel.SENIOR,
                    "Müşteri Hizmetleri", "Kıdemli Lead",
                    Set.of("TR", "EN", "AR"),
                    Set.of("HAIR_TRANSPLANT", "RHINOPLASTY"),
                    "https://i.pravatar.cc/150?img=47"),

            new TeamMember(
                    "ahmet.demir@healthvia.com", "+905550000005", "Ahmet", "Demir",
                    UserRole.AGENT, AdminLevel.STANDARD,
                    "Müşteri Hizmetleri", "Lead",
                    Set.of("TR", "EN", "DE"),
                    Set.of("CARDIOLOGY", "ORTHOPEDICS"),
                    "https://i.pravatar.cc/150?img=12"),

            new TeamMember(
                    "elena.rossi@healthvia.com", "+905550000006", "Elena", "Rossi",
                    UserRole.AGENT, AdminLevel.STANDARD,
                    "Müşteri Hizmetleri", "Lead",
                    Set.of("IT", "EN", "FR"),
                    Set.of("IVF", "DENTAL"),
                    "https://i.pravatar.cc/150?img=45"),

            new TeamMember(
                    "omar.hassan@healthvia.com", "+905550000007", "Omar", "Hassan",
                    UserRole.AGENT, AdminLevel.STANDARD,
                    "Müşteri Hizmetleri", "Lead",
                    Set.of("AR", "EN", "TR"),
                    Set.of("BBL", "RHINOPLASTY", "HAIR_TRANSPLANT"),
                    "https://i.pravatar.cc/150?img=33"),

            new TeamMember(
                    "sofia.meyer@healthvia.com", "+905550000008", "Sofia", "Meyer",
                    UserRole.AGENT, AdminLevel.STANDARD,
                    "Müşteri Hizmetleri", "Lead",
                    Set.of("DE", "EN", "TR"),
                    Set.of("BARIATRIC", "ORTHOPEDICS", "LASIK"),
                    "https://i.pravatar.cc/150?img=44")
    );

    /** Agent email listesi — LeadSeederRunner tarafından kullanılır. */
    public static List<String> agentSeedEmails() {
        return TEAM_SEED.stream()
                .filter(t -> t.role == UserRole.AGENT)
                .map(t -> t.email)
                .toList();
    }

    private static Language toLanguage(String code) {
        if (code == null) return Language.ENGLISH;
        return switch (code.toUpperCase()) {
            case "TR" -> Language.TURKISH;
            case "AR" -> Language.ARABIC;
            case "DE" -> Language.GERMAN;
            case "FR" -> Language.FRENCH;
            case "RU" -> Language.RUSSIAN;
            case "EN" -> Language.ENGLISH;
            default -> Language.ENGLISH;
        };
    }
}
