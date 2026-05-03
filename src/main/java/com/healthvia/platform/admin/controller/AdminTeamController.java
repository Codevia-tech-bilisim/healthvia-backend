package com.healthvia.platform.admin.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.enums.UserRole;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Lightweight team-management endpoints for the agent dashboard.
 *
 *   GET    /api/v1/admins                  → list all HealthVia employees
 *                                            (used by Team page + transfer
 *                                            modal + dispatcher dropdowns)
 *   PATCH  /api/v1/admins/{id}/availability
 *          ?available=true|false
 *          &maxConcurrentChats=8           → toggle online + capacity
 *                                            (manager / SUPERADMIN only)
 *
 * The legacy /api/admin/admins (paged) controller stays untouched — this
 * file just provides the simpler shape the frontend already expects.
 */
@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','CEO','AGENT')")
public class AdminTeamController {

    private final AdminRepository adminRepository;

    @GetMapping
    public ApiResponse<List<TeamMemberDto>> listTeam(
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "role", required = false) UserRole role) {
        List<Admin> all = adminRepository.findAll().stream()
            .filter(a -> !a.isDeleted())
            .filter(a -> active == null || (active.equals(a.getIsAvailable())))
            .filter(a -> role == null || a.getRole() == role)
            .toList();
        return ApiResponse.success(all.stream().map(TeamMemberDto::from).toList());
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','CEO') or #id == authentication.principal.id")
    public ApiResponse<TeamMemberDto> setAvailability(
            @PathVariable String id,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer maxConcurrentChats) {
        Admin a = adminRepository.findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + id));
        if (available != null) a.setIsAvailable(available);
        if (maxConcurrentChats != null && maxConcurrentChats >= 1 && maxConcurrentChats <= 30) {
            a.setMaxConcurrentChats(maxConcurrentChats);
        }
        Admin saved = adminRepository.save(a);
        return ApiResponse.success(TeamMemberDto.from(saved), "Müsaitlik güncellendi");
    }

    /* ---------- DTO ---------- */

    @Data
    @Builder
    public static class TeamMemberDto {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private UserRole role;
        private String avatarUrl;
        private String department;
        private String jobTitle;
        private List<String> languages;
        private List<String> specializations;
        private Boolean active;
        private Integer currentActiveChats;
        private Integer maxConcurrentChats;
        private Integer assignedLeadCount;
        private Integer convertedLeadCount;

        static TeamMemberDto from(Admin a) {
            return TeamMemberDto.builder()
                .id(a.getId())
                .firstName(a.getFirstName())
                .lastName(a.getLastName())
                .email(a.getEmail())
                .phone(a.getPhone())
                .role(a.getRole())
                .avatarUrl(a.getAvatarUrl())
                .department(a.getDepartment())
                .jobTitle(a.getJobTitle())
                .languages(a.getSpokenLanguages() == null
                    ? List.of()
                    : a.getSpokenLanguages().stream().toList())
                .specializations(a.getSpecializations() == null
                    ? List.of()
                    : a.getSpecializations().stream().toList())
                .active(a.getIsAvailable())
                .currentActiveChats(a.getCurrentActiveChats())
                .maxConcurrentChats(a.getMaxConcurrentChats())
                .assignedLeadCount(a.getAssignedLeadCount())
                .convertedLeadCount(a.getConvertedLeadCount())
                .build();
        }
    }
}
