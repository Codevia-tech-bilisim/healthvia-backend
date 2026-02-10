// admin/dto/AdminDto.java
package com.healthvia.platform.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.common.enums.Language;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDto {
    
    // === USER FIELDS ===
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private User.Gender gender;
    private LocalDate birthDate;
    private String province;
    private String district;
    private UserRole role;
    private UserStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Integer profileCompletionRate;
    private LocalDateTime lastLoginDate;
    private Language preferredLanguage;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // === ADMIN SPECIFIC FIELDS ===
    private String department;
    private String jobTitle;
    private Admin.AdminLevel adminLevel;
    
    // === PERMISSIONS ===
    private Set<Admin.AdminPermission> permissions;
    private Boolean canManageUsers;
    private Boolean canManageDoctors;
    private Boolean canManageClinics;
    private Boolean canViewReports;
    private Boolean canManageSystem;
    
    // === EMPLOYEE INFO ===
    private String employeeId;
    private LocalDateTime hireDate;
    private String supervisorId;
    private String supervisorName; // Computed field

        // === SATIŞ TEMSİLCİSİ ===
    private Set<String> spokenLanguages;
    private Boolean isAvailable;
    private Integer maxConcurrentChats;
    private Integer currentActiveChats;
    private Integer assignedLeadCount;
    private Integer convertedLeadCount;
    private Long averageResponseTimeSeconds;
    private Set<String> specializations;
    private java.time.LocalTime shiftStart;
    private java.time.LocalTime shiftEnd;
    private Set<String> workingDays;

    // Computed
    private Double leadConversionRate;
    private Boolean canAcceptNewChat;
    
    // === ACTIVITY TRACKING ===
    private LocalDateTime lastAdminAction;
    private Integer totalActionsPerformed;
    private Integer usersManaged;
    private Integer doctorsApproved;
    private Integer clinicsApproved;
    
    // === COMPUTED METHODS ===
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getAge() {
        if (birthDate == null) return 0;
        return LocalDate.now().getYear() - birthDate.getYear();
    }
    
    public boolean isSuperAdmin() {
        return Admin.AdminLevel.SUPER_ADMIN.equals(adminLevel);
    }
    
    public boolean isManager() {
        return adminLevel != null && 
               (adminLevel == Admin.AdminLevel.MANAGER || adminLevel == Admin.AdminLevel.SUPER_ADMIN);
    }
    
    public boolean canPerformAction(Admin.AdminPermission permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    public boolean canManageUserType(String userType) {
        switch (userType.toUpperCase()) {
            case "PATIENT":
                return getCanManageUsers();
            case "DOCTOR":
                return getCanManageDoctors();
            case "CLINIC":
                return getCanManageClinics();
            case "ADMIN":
                return isSuperAdmin();
            default:
                return false;
        }
    }
    
    public String getAdminDisplayName() {
        return String.format("%s - %s (%s)", 
            getFullName(), 
            jobTitle != null ? jobTitle : "Admin", 
            department != null ? department : "Genel"
        );
    }
    
    public String getAdminLevelDisplay() {
        return adminLevel != null ? adminLevel.getDisplayName() : "Belirtilmemiş";
    }
    
    public String getExperienceDisplay() {
        if (hireDate == null) return "Belirtilmemiş";
        
        LocalDateTime now = LocalDateTime.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(hireDate, now);
        
        if (days < 30) {
            return days + " gün";
        } else if (days < 365) {
            return (days / 30) + " ay";
        } else {
            return (days / 365) + " yıl";
        }
    }
    
    public boolean isActiveAdmin() {
        if (lastAdminAction == null) return false;
        return lastAdminAction.isAfter(LocalDateTime.now().minusDays(7));
    }
    
    public double getActionPerformanceScore() {
        if (totalActionsPerformed == null || hireDate == null) return 0.0;
        
        long workingDays = java.time.temporal.ChronoUnit.DAYS.between(hireDate, LocalDateTime.now());
        if (workingDays == 0) return 0.0;
        
        return totalActionsPerformed.doubleValue() / workingDays;
    }
    
    public String getPermissionSummary() {
        if (permissions == null || permissions.isEmpty()) {
            return "Temel yetkiler";
        }
        return permissions.size() + " özel yetki";
    }
    
    // === FACTORY METHODS ===
    
    public static AdminDto fromEntity(Admin admin) {
        if (admin == null) return null;
        
        return AdminDto.builder()
            // User fields
            .id(admin.getId())
            .firstName(admin.getFirstName())
            .lastName(admin.getLastName())
            .email(admin.getEmail())
            .phone(admin.getPhone())
            .gender(admin.getGender())
            .birthDate(admin.getBirthDate())
            .province(admin.getProvince())
            .district(admin.getDistrict())
            .role(admin.getRole())
            .status(admin.getStatus())
            .emailVerified(admin.getEmailVerified())
            .phoneVerified(admin.getPhoneVerified())
            .profileCompletionRate(admin.getProfileCompletionRate())
            .lastLoginDate(admin.getLastLoginDate())
            .preferredLanguage(admin.getPreferredLanguage())
            .avatarUrl(admin.getAvatarUrl())
            .createdAt(admin.getCreatedAt())
            .updatedAt(admin.getUpdatedAt())
            
            // Admin specific fields
            .department(admin.getDepartment())
            .jobTitle(admin.getJobTitle())
            .adminLevel(admin.getAdminLevel())
            .permissions(admin.getPermissions())
            .canManageUsers(admin.getCanManageUsers())
            .canManageDoctors(admin.getCanManageDoctors())
            .canManageClinics(admin.getCanManageClinics())
            .canViewReports(admin.getCanViewReports())
            .canManageSystem(admin.getCanManageSystem())
            .employeeId(admin.getEmployeeId())
            .hireDate(admin.getHireDate())
            .supervisorId(admin.getSupervisorId())
            .lastAdminAction(admin.getLastAdminAction())
            .totalActionsPerformed(admin.getTotalActionsPerformed())
            .usersManaged(admin.getUsersManaged())
            .doctorsApproved(admin.getDoctorsApproved())
            .clinicsApproved(admin.getClinicsApproved())
            .spokenLanguages(admin.getSpokenLanguages())
            .isAvailable(admin.getIsAvailable())
            .maxConcurrentChats(admin.getMaxConcurrentChats())
            .currentActiveChats(admin.getCurrentActiveChats())
            .assignedLeadCount(admin.getAssignedLeadCount())
            .convertedLeadCount(admin.getConvertedLeadCount())
            .averageResponseTimeSeconds(admin.getAverageResponseTimeSeconds())
            .specializations(admin.getSpecializations())
            .shiftStart(admin.getShiftStart())
            .shiftEnd(admin.getShiftEnd())
            .workingDays(admin.getWorkingDays())
            .leadConversionRate(admin.getLeadConversionRate())
            .canAcceptNewChat(admin.canAcceptNewChat())
            .build();
    }
    
    public static AdminDto fromEntityBasic(Admin admin) {
        if (admin == null) return null;
        
        return AdminDto.builder()
            .id(admin.getId())
            .firstName(admin.getFirstName())
            .lastName(admin.getLastName())
            .email(admin.getEmail())
            .department(admin.getDepartment())
            .jobTitle(admin.getJobTitle())
            .adminLevel(admin.getAdminLevel())
            .employeeId(admin.getEmployeeId())
            .status(admin.getStatus())
            .avatarUrl(admin.getAvatarUrl())
            .lastAdminAction(admin.getLastAdminAction())
            .build();
    }
    
    public static AdminDto fromEntityWithSupervisor(Admin admin, String supervisorName) {
        AdminDto dto = fromEntity(admin);
        if (dto != null) {
            dto.setSupervisorName(supervisorName);
        }
        return dto;
    }
    
    public Admin toEntity() {
        Admin admin = Admin.builder()
            // User fields
            .id(this.id)
            .firstName(this.firstName)
            .lastName(this.lastName)
            .email(this.email)
            .phone(this.phone)
            .gender(this.gender)
            .birthDate(this.birthDate)
            .province(this.province)
            .district(this.district)
            .role(this.role)
            .status(this.status)
            .emailVerified(this.emailVerified)
            .phoneVerified(this.phoneVerified)
            .profileCompletionRate(this.profileCompletionRate)
            .lastLoginDate(this.lastLoginDate)
            .preferredLanguage(this.preferredLanguage)
            .avatarUrl(this.avatarUrl)
            
            // Admin specific fields
            .department(this.department)
            .jobTitle(this.jobTitle)
            .adminLevel(this.adminLevel)
            .permissions(this.permissions)
            .canManageUsers(this.canManageUsers)
            .canManageDoctors(this.canManageDoctors)
            .canManageClinics(this.canManageClinics)
            .canViewReports(this.canViewReports)
            .canManageSystem(this.canManageSystem)
            .employeeId(this.employeeId)
            .hireDate(this.hireDate)
            .supervisorId(this.supervisorId)
            .lastAdminAction(this.lastAdminAction)
            .totalActionsPerformed(this.totalActionsPerformed)
            .usersManaged(this.usersManaged)
            .doctorsApproved(this.doctorsApproved)
            .clinicsApproved(this.clinicsApproved)
            .build();
            
        return admin;
    }
    
    // === GETTERS WITH DEFAULTS ===
    
    public Boolean getCanManageUsers() {
        return canManageUsers != null ? canManageUsers : false;
    }
    
    public Boolean getCanManageDoctors() {
        return canManageDoctors != null ? canManageDoctors : false;
    }
    
    public Boolean getCanManageClinics() {
        return Boolean.TRUE.equals(canManageClinics);
    }
    
    public Boolean getCanViewReports() {
        return canViewReports != null ? canViewReports : false;
    }
    
    public Boolean getCanManageSystem() {
        return canManageSystem != null ? canManageSystem : false;
    }
    
    public Integer getTotalActionsPerformed() {
        return totalActionsPerformed != null ? totalActionsPerformed : 0;
    }
    
    public Integer getUsersManaged() {
        return usersManaged != null ? usersManaged : 0;
    }
    
    public Integer getDoctorsApproved() {
        return doctorsApproved != null ? doctorsApproved : 0;
    }
    
    public Integer getClinicsApproved() {
        return clinicsApproved != null ? clinicsApproved : 0;
    }
}