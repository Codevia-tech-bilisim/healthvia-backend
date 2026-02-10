// admin/service/impl/AdminServiceImpl.java
package com.healthvia.platform.admin.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.admin.service.AdminService;
import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    @Override
    public Admin createAdmin(Admin admin) {
        validateAdminForCreation(admin);
        
        if (!isEmployeeIdAvailable(admin.getEmployeeId())) {
            throw new BusinessException(null, "Employee ID already exists");
        }
        
        // Default values
        if (admin.getAdminLevel() == null) {
            admin.setAdminLevel(Admin.AdminLevel.STANDARD);
        }
        if (admin.getCanManageUsers() == null) {
            admin.setCanManageUsers(true);
        }
        if (admin.getHireDate() == null) {
            admin.setHireDate(LocalDateTime.now());
        }
        
        return adminRepository.save(admin);
    }

    @Override
    public Admin updateAdmin(String id, Admin admin) {
        Admin existingAdmin = findByIdOrThrow(id);
        updateAdminFields(existingAdmin, admin);
        return adminRepository.save(existingAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Admin> findById(String id) {
        return adminRepository.findById(id).filter(admin -> !admin.isDeleted());
    }

    @Override
    public void deleteAdmin(String id, String deletedBy) {
        Admin admin = findByIdOrThrow(id);
        
        // Super admin cannot be deleted
        if (admin.isSuperAdmin()) {
            throw new BusinessException(null, "Super admin cannot be deleted");
        }
        
        admin.markAsDeleted(deletedBy);
        adminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Admin> findAll(Pageable pageable) {
        return adminRepository.findAll(pageable);
    }

    // === ADMIN PROFILE MANAGEMENT ===

    @Override
    public Admin updateProfile(String adminId, String department, String jobTitle) {
        Admin admin = findByIdOrThrow(adminId);
        admin.setDepartment(department);
        admin.setJobTitle(jobTitle);
        return adminRepository.save(admin);
    }

    @Override
    public Admin updatePermissions(String adminId, Set<Admin.AdminPermission> permissions) {
        Admin admin = findByIdOrThrow(adminId);
        admin.setPermissions(permissions);
        return adminRepository.save(admin);
    }

    @Override
    public Admin updateCapabilities(String adminId, Boolean canManageUsers, Boolean canManageDoctors, 
                                  Boolean canManageClinics, Boolean canViewReports, Boolean canManageSystem) {
        Admin admin = findByIdOrThrow(adminId);
        
        if (canManageUsers != null) admin.setCanManageUsers(canManageUsers);
        if (canManageDoctors != null) admin.setCanManageDoctors(canManageDoctors);
        if (canManageClinics != null) admin.setCanManageClinics(canManageClinics);
        if (canViewReports != null) admin.setCanViewReports(canViewReports);
        if (canManageSystem != null) admin.setCanManageSystem(canManageSystem);
        
        return adminRepository.save(admin);
    }

    // === ADMIN HIERARCHY ===

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findSubordinates(String supervisorId) {
        return adminRepository.findBySupervisorIdAndDeletedFalse(supervisorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findTopLevelAdmins() {
        return adminRepository.findTopLevelAdmins();
    }

    @Override
    public Admin assignSupervisor(String adminId, String supervisorId) {
        Admin admin = findByIdOrThrow(adminId);
        Admin supervisor = findByIdOrThrow(supervisorId);
        
        // Check hierarchy validity
        if (admin.getId().equals(supervisorId)) {
            throw new BusinessException(null, "Admin cannot be supervisor of themselves");
        }
        
        admin.setSupervisorId(supervisorId);
        return adminRepository.save(admin);
    }

    // === SEARCH & FILTER ===

    @Override
    @Transactional(readOnly = true)
    public Page<Admin> searchAdmins(String searchTerm, Pageable pageable) {
        return adminRepository.searchAdmins(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findByDepartment(String department) {
        return adminRepository.findByDepartmentAndDeletedFalse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findByAdminLevel(Admin.AdminLevel adminLevel) {
        return adminRepository.findByAdminLevelAndDeletedFalse(adminLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findByPermission(Admin.AdminPermission permission) {
        return adminRepository.findAdminsWithPermission(permission);
    }

    // === ACTIVITY TRACKING ===

    @Override
    public Admin recordAdminAction(String adminId) {
        Admin admin = findByIdOrThrow(adminId);
        admin.recordAdminAction();
        return adminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findActiveAdminsSince(LocalDateTime since) {
        return adminRepository.findActiveAdminsSince(since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findInactiveAdminsSince(LocalDateTime since) {
        return adminRepository.findInactiveAdminsSince(since);
    }

    // === PERMISSION MANAGEMENT ===

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(String adminId, Admin.AdminPermission permission) {
        Admin admin = findByIdOrThrow(adminId);
        return admin.canPerformAction(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageUserType(String adminId, String userType) {
        Admin admin = findByIdOrThrow(adminId);
        return admin.canManageUserType(userType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findAdminsWithPermission(Admin.AdminPermission permission) {
        return adminRepository.findAdminsWithPermission(permission);
    }

    // === STATISTICS & ANALYTICS ===

    @Override
    @Transactional(readOnly = true)
    public long countAdminsByDepartment(String department) {
        return adminRepository.countByDepartmentAndDeletedFalse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAdminsByLevel(Admin.AdminLevel level) {
        return adminRepository.countByAdminLevelAndDeletedFalse(level);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findMostActiveAdmins(int minActions) {
        return adminRepository.findMostActiveAdmins(minActions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findHighPerformingAdmins(int minTotalActions) {
        return adminRepository.findHighPerformingAdmins(minTotalActions);
    }

    // === VALIDATION ===

    @Override
    @Transactional(readOnly = true)
    public boolean isEmployeeIdAvailable(String employeeId) {
        return !adminRepository.existsByEmployeeIdAndDeletedFalse(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canPerformSystemOperation(String adminId) {
        Admin admin = findByIdOrThrow(adminId);
        return admin.getCanManageSystem() || admin.isSuperAdmin();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSuperAdmin(String adminId) {
        Admin admin = findByIdOrThrow(adminId);
        return admin.isSuperAdmin();
    }

        // === AGENT YÖNETİMİ ===

    @Override
    public Admin updateAgentProfile(String adminId, Set<String> spokenLanguages,
                                   Set<String> specializations, Integer maxConcurrentChats) {
        Admin admin = findByIdOrThrow(adminId);
        if (spokenLanguages != null) admin.setSpokenLanguages(spokenLanguages);
        if (specializations != null) admin.setSpecializations(specializations);
        if (maxConcurrentChats != null) admin.setMaxConcurrentChats(maxConcurrentChats);
        return adminRepository.save(admin);
    }

    @Override
    public Admin setAvailability(String adminId, boolean isAvailable) {
        Admin admin = findByIdOrThrow(adminId);
        admin.setIsAvailable(isAvailable);
        if (!isAvailable) {
            admin.setCurrentActiveChats(0);
        }
        return adminRepository.save(admin);
    }

    @Override
    public Admin updateShift(String adminId, java.time.LocalTime shiftStart,
                            java.time.LocalTime shiftEnd, Set<String> workingDays) {
        Admin admin = findByIdOrThrow(adminId);
        admin.setShiftStart(shiftStart);
        admin.setShiftEnd(shiftEnd);
        admin.setWorkingDays(workingDays);
        return adminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findAvailableAgents() {
        return adminRepository.findByIsAvailableTrueAndDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findAvailableAgentsByLanguage(String language) {
        return adminRepository.findAvailableAgentsByLanguage(language);
    }

    @Override
    @Transactional(readOnly = true)
    public Admin findBestAvailableAgent(String language, String treatmentCategory) {
        List<Admin> agents = findAvailableAgents();

        return agents.stream()
                .filter(Admin::canAcceptNewChat)
                .filter(a -> language == null || a.speaksLanguage(language))
                .filter(a -> treatmentCategory == null || a.isSpecializedIn(treatmentCategory))
                .min(java.util.Comparator.comparingInt(Admin::getCurrentActiveChats))
                .orElse(null); // En az yükü olan agent
    }

    // === HELPER METHODS ===

    private Admin findByIdOrThrow(String id) {
        return adminRepository.findById(id)
            .filter(admin -> !admin.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", id));
    }

    private void validateAdminForCreation(Admin admin) {
        if (admin.getDepartment() == null || admin.getDepartment().trim().isEmpty()) {
            throw new BusinessException(null, "Department is required");
        }
        if (admin.getEmployeeId() == null || admin.getEmployeeId().trim().isEmpty()) {
            throw new BusinessException(null, "Employee ID is required");
        }
    }

    private void updateAdminFields(Admin existingAdmin, Admin newAdmin) {
        if (newAdmin.getDepartment() != null) {
            existingAdmin.setDepartment(newAdmin.getDepartment());
        }
        if (newAdmin.getJobTitle() != null) {
            existingAdmin.setJobTitle(newAdmin.getJobTitle());
        }
        if (newAdmin.getAdminLevel() != null) {
            existingAdmin.setAdminLevel(newAdmin.getAdminLevel());
        }
        if (newAdmin.getPermissions() != null) {
            existingAdmin.setPermissions(newAdmin.getPermissions());
        }
        if (newAdmin.getSupervisorId() != null) {
            existingAdmin.setSupervisorId(newAdmin.getSupervisorId());
        }
    }
}
