// admin/controller/AdminController.java
package com.healthvia.platform.admin.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.admin.dto.AdminDto;
import com.healthvia.platform.admin.dto.AdminPermissionDto;
import com.healthvia.platform.admin.dto.AdminSummaryDto;
import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.service.AdminService;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // === ADMIN PROFILE MANAGEMENT ===
    
    @GetMapping("/me")
    public ApiResponse<AdminDto> getMyProfile() {
        String adminId = SecurityUtils.getCurrentUserId();
        return adminService.findById(adminId)
            .map(AdminDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Admin profile not found"));
    }
    
    @PatchMapping("/me/profile")
    public ApiResponse<AdminDto> updateMyProfile(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String jobTitle) {
        String adminId = SecurityUtils.getCurrentUserId();
        Admin updatedAdmin = adminService.updateProfile(adminId, department, jobTitle);
        return ApiResponse.success(AdminDto.fromEntity(updatedAdmin), "Profile updated successfully");
    }
    
    @PatchMapping("/me/record-action")
    public ApiResponse<AdminDto> recordMyAction() {
        String adminId = SecurityUtils.getCurrentUserId();
        Admin updatedAdmin = adminService.recordAdminAction(adminId);
        return ApiResponse.success(AdminDto.fromEntity(updatedAdmin), "Action recorded");
    }

    // === ADMIN MANAGEMENT (SUPER ADMIN ONLY) ===
    
    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<Page<AdminDto>> getAllAdmins(@PageableDefault(size = 20) Pageable pageable) {
        Page<Admin> admins = adminService.findAll(pageable);
        Page<AdminDto> adminDtos = admins.map(AdminDto::fromEntity);
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/admins/search")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<Page<AdminDto>> searchAdmins(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Admin> admins = adminService.searchAdmins(searchTerm, pageable);
        Page<AdminDto> adminDtos = admins.map(AdminDto::fromEntity);
        return ApiResponse.success(adminDtos);
    }
    
    @PostMapping("/admins")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<AdminDto> createAdmin(@Valid @RequestBody Admin admin) {
        Admin createdAdmin = adminService.createAdmin(admin);
        return ApiResponse.success(AdminDto.fromEntity(createdAdmin), "Admin created successfully");
    }
    
    @GetMapping("/admins/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDto> getAdminById(@PathVariable String id) {
        return adminService.findById(id)
            .map(AdminDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Admin not found"));
    }
    
    @PatchMapping("/admins/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<AdminDto> updateAdminPermissions(
            @PathVariable String id,
            @RequestBody Set<Admin.AdminPermission> permissions) {
        Admin updatedAdmin = adminService.updatePermissions(id, permissions);
        return ApiResponse.success(AdminDto.fromEntity(updatedAdmin), "Permissions updated successfully");
    }
    
    @PatchMapping("/admins/{id}/capabilities")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<AdminDto> updateAdminCapabilities(
            @PathVariable String id,
            @RequestParam(required = false) Boolean canManageUsers,
            @RequestParam(required = false) Boolean canManageDoctors,
            @RequestParam(required = false) Boolean canManageClinics,
            @RequestParam(required = false) Boolean canViewReports,
            @RequestParam(required = false) Boolean canManageSystem) {
        
        Admin updatedAdmin = adminService.updateCapabilities(
            id, canManageUsers, canManageDoctors, canManageClinics, canViewReports, canManageSystem);
        return ApiResponse.success(AdminDto.fromEntity(updatedAdmin), "Capabilities updated successfully");
    }
    
    @PostMapping("/admins/{id}/assign-supervisor")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<AdminDto> assignSupervisor(
            @PathVariable String id,
            @RequestParam String supervisorId) {
        Admin updatedAdmin = adminService.assignSupervisor(id, supervisorId);
        return ApiResponse.success(AdminDto.fromEntity(updatedAdmin), "Supervisor assigned successfully");
    }
    
    @DeleteMapping("/admins/{id}")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isSuperAdmin(authentication.name)")
    public ApiResponse<Void> deleteAdmin(@PathVariable String id) {
        String deletedBy = SecurityUtils.getCurrentUserId();
        adminService.deleteAdmin(id, deletedBy);
        return ApiResponse.success("Admin deleted successfully");
    }

    // === HIERARCHY MANAGEMENT ===
    
    @GetMapping("/admins/top-level")
    public ApiResponse<List<AdminSummaryDto>> getTopLevelAdmins() {
        List<Admin> admins = adminService.findTopLevelAdmins();
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/admins/{id}/subordinates")
    public ApiResponse<List<AdminSummaryDto>> getSubordinates(@PathVariable String id) {
        List<Admin> subordinates = adminService.findSubordinates(id);
        List<AdminSummaryDto> adminDtos = subordinates.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/me/subordinates")
    public ApiResponse<List<AdminSummaryDto>> getMySubordinates() {
        String adminId = SecurityUtils.getCurrentUserId();
        List<Admin> subordinates = adminService.findSubordinates(adminId);
        List<AdminSummaryDto> adminDtos = subordinates.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }

    // === SEARCH & FILTER ===
    
    @GetMapping("/admins/by-department/{department}")
    public ApiResponse<List<AdminSummaryDto>> getAdminsByDepartment(@PathVariable String department) {
        List<Admin> admins = adminService.findByDepartment(department);
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/admins/by-level/{level}")
    public ApiResponse<List<AdminSummaryDto>> getAdminsByLevel(@PathVariable Admin.AdminLevel level) {
        List<Admin> admins = adminService.findByAdminLevel(level);
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/admins/by-permission/{permission}")
    public ApiResponse<List<AdminPermissionDto>> getAdminsByPermission(@PathVariable Admin.AdminPermission permission) {
        List<Admin> admins = adminService.findByPermission(permission);
        List<AdminPermissionDto> adminDtos = admins.stream()
            .map(AdminPermissionDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }

    // === ACTIVITY TRACKING ===
    
    @GetMapping("/admins/active")
    public ApiResponse<List<AdminSummaryDto>> getActiveAdmins(
            @RequestParam(defaultValue = "24") int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        List<Admin> admins = adminService.findActiveAdminsSince(since);
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/admins/inactive")
    public ApiResponse<List<AdminSummaryDto>> getInactiveAdmins(
            @RequestParam(defaultValue = "72") int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        List<Admin> admins = adminService.findInactiveAdminsSince(since);
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }
    
    @GetMapping("/admins/most-active")
    public ApiResponse<List<AdminSummaryDto>> getMostActiveAdmins(
            @RequestParam(defaultValue = "10") int minActions) {
        List<Admin> admins = adminService.findMostActiveAdmins(minActions);
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }

    // === PERMISSION CHECKS ===
    
    @GetMapping("/me/permissions/check/{permission}")
    public ApiResponse<Boolean> checkMyPermission(@PathVariable Admin.AdminPermission permission) {
        String adminId = SecurityUtils.getCurrentUserId();
        boolean hasPermission = adminService.hasPermission(adminId, permission);
        return ApiResponse.success(hasPermission);
    }
    
    @GetMapping("/me/permissions/user-type/{userType}")
    public ApiResponse<Boolean> checkUserTypePermission(@PathVariable String userType) {
        String adminId = SecurityUtils.getCurrentUserId();
        boolean canManage = adminService.canManageUserType(adminId, userType);
        return ApiResponse.success(canManage);
    }
    
    @GetMapping("/me/permissions/system-operations")
    public ApiResponse<Boolean> checkSystemOperationPermission() {
        String adminId = SecurityUtils.getCurrentUserId();
        boolean canPerform = adminService.canPerformSystemOperation(adminId);
        return ApiResponse.success(canPerform);
    }

        // === AGENT MANAGEMENT ===

    @PatchMapping("/me/availability")
    public ApiResponse<AdminDto> setMyAvailability(@RequestParam boolean available) {
        String adminId = SecurityUtils.getCurrentUserId();
        Admin updated = adminService.setAvailability(adminId, available);
        return ApiResponse.success(AdminDto.fromEntity(updated),
                available ? "Çevrimiçi oldunuz" : "Çevrimdışı oldunuz");
    }

    @PatchMapping("/me/agent-profile")
    public ApiResponse<AdminDto> updateMyAgentProfile(
            @RequestParam(required = false) Set<String> spokenLanguages,
            @RequestParam(required = false) Set<String> specializations,
            @RequestParam(required = false) Integer maxConcurrentChats) {

        String adminId = SecurityUtils.getCurrentUserId();
        Admin updated = adminService.updateAgentProfile(adminId, spokenLanguages,
                specializations, maxConcurrentChats);
        return ApiResponse.success(AdminDto.fromEntity(updated), "Agent profili güncellendi");
    }

    @PatchMapping("/me/shift")
    public ApiResponse<AdminDto> updateMyShift(
            @RequestParam java.time.LocalTime shiftStart,
            @RequestParam java.time.LocalTime shiftEnd,
            @RequestParam Set<String> workingDays) {

        String adminId = SecurityUtils.getCurrentUserId();
        Admin updated = adminService.updateShift(adminId, shiftStart, shiftEnd, workingDays);
        return ApiResponse.success(AdminDto.fromEntity(updated), "Mesai saatleri güncellendi");
    }

    @GetMapping("/agents/available")
    public ApiResponse<List<AdminSummaryDto>> getAvailableAgents() {
        List<Admin> agents = adminService.findAvailableAgents();
        List<AdminSummaryDto> dtos = agents.stream()
                .map(AdminSummaryDto::fromEntity)
                .toList();
        return ApiResponse.success(dtos);
    }

    @GetMapping("/agents/available/language/{language}")
    public ApiResponse<List<AdminSummaryDto>> getAvailableAgentsByLanguage(
            @PathVariable String language) {
        List<Admin> agents = adminService.findAvailableAgentsByLanguage(language);
        List<AdminSummaryDto> dtos = agents.stream()
                .map(AdminSummaryDto::fromEntity)
                .toList();
        return ApiResponse.success(dtos);
    }

    // === STATISTICS ===
    
    @GetMapping("/statistics/count-by-department")
    public ApiResponse<Long> getAdminsCountByDepartment(@RequestParam String department) {
        long count = adminService.countAdminsByDepartment(department);
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/count-by-level")
    public ApiResponse<Long> getAdminsCountByLevel(@RequestParam Admin.AdminLevel level) {
        long count = adminService.countAdminsByLevel(level);
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/high-performing")
    public ApiResponse<List<AdminSummaryDto>> getHighPerformingAdmins(
            @RequestParam(defaultValue = "50") int minTotalActions) {
        List<Admin> admins = adminService.findHighPerformingAdmins(minTotalActions);
        List<AdminSummaryDto> adminDtos = admins.stream()
            .map(AdminSummaryDto::fromEntity)
            .toList();
        return ApiResponse.success(adminDtos);
    }

    // === VALIDATION ===
    
    @GetMapping("/validation/employee-id")
    public ApiResponse<Boolean> checkEmployeeIdAvailability(@RequestParam String employeeId) {
        boolean available = adminService.isEmployeeIdAvailable(employeeId);
        return ApiResponse.success(available);
    }
}