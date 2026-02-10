package com.healthvia.platform.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.admin.entity.Admin;

public interface AdminService {

    // === BASIC CRUD OPERATIONS ===
    Admin createAdmin(Admin admin);
    Admin updateAdmin(String id, Admin admin);
    Optional<Admin> findById(String id);
    void deleteAdmin(String id, String deletedBy);
    Page<Admin> findAll(Pageable pageable);

    // === ADMIN PROFILE MANAGEMENT ===
    Admin updateProfile(String adminId, String department, String jobTitle);
    Admin updatePermissions(String adminId, Set<Admin.AdminPermission> permissions);
    Admin updateCapabilities(String adminId, Boolean canManageUsers, Boolean canManageDoctors, 
                           Boolean canManageClinics, Boolean canViewReports, Boolean canManageSystem);

    // === ADMIN HIERARCHY ===
    List<Admin> findSubordinates(String supervisorId);
    List<Admin> findTopLevelAdmins();
    Admin assignSupervisor(String adminId, String supervisorId);

    // === SEARCH & FILTER ===
    Page<Admin> searchAdmins(String searchTerm, Pageable pageable);
    List<Admin> findByDepartment(String department);
    List<Admin> findByAdminLevel(Admin.AdminLevel adminLevel);
    List<Admin> findByPermission(Admin.AdminPermission permission);

    // === ACTIVITY TRACKING ===
    Admin recordAdminAction(String adminId);
    List<Admin> findActiveAdminsSince(LocalDateTime since);
    List<Admin> findInactiveAdminsSince(LocalDateTime since);

    // === PERMISSION MANAGEMENT ===
    boolean hasPermission(String adminId, Admin.AdminPermission permission);
    boolean canManageUserType(String adminId, String userType);
    List<Admin> findAdminsWithPermission(Admin.AdminPermission permission);

    // === STATISTICS & ANALYTICS ===
    long countAdminsByDepartment(String department);
    long countAdminsByLevel(Admin.AdminLevel level);
    List<Admin> findMostActiveAdmins(int minActions);
    List<Admin> findHighPerformingAdmins(int minTotalActions);

        // === AGENT (SATIŞ TEMSİLCİSİ) YÖNETİMİ ===
    Admin updateAgentProfile(String adminId, Set<String> spokenLanguages,
                            Set<String> specializations, Integer maxConcurrentChats);
    Admin setAvailability(String adminId, boolean isAvailable);
    Admin updateShift(String adminId, java.time.LocalTime shiftStart,
                     java.time.LocalTime shiftEnd, Set<String> workingDays);
    List<Admin> findAvailableAgents();
    List<Admin> findAvailableAgentsByLanguage(String language);
    Admin findBestAvailableAgent(String language, String treatmentCategory);

    // === VALIDATION ===
    boolean isEmployeeIdAvailable(String employeeId);
    boolean canPerformSystemOperation(String adminId);
    boolean isSuperAdmin(String adminId);
}