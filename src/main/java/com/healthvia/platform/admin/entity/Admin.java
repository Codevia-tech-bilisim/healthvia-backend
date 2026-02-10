// user/entity/Admin.java
package com.healthvia.platform.admin.entity;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.user.entity.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "admins")
public class Admin extends User {

    // === ADMIN ÖZELLİKLERİ (Minimal) ===
    
    @NotBlank(message = "Departman bilgisi boş olamaz")
    @Size(max = 100, message = "Departman adı en fazla 100 karakter olabilir")
    private String department; // IT, Sağlık Yönetimi, Mali İşler vs.

    @Field("job_title")
    @Size(max = 100, message = "Ünvan en fazla 100 karakter olabilir")
    private String jobTitle; // Sistem Yöneticisi, Platform Müdürü vs.

    @Field("admin_level")
    private AdminLevel adminLevel;

    // === YETKİLER ===
    @Field("permissions")
    private Set<AdminPermission> permissions;

    @Field("can_manage_users")
    private Boolean canManageUsers;

    @Field("can_manage_doctors")
    private Boolean canManageDoctors;

    @Field("can_manage_clinics")
    private Boolean canManageClinics;

    @Field("can_view_reports")
    private Boolean canViewReports;

    @Field("can_manage_system")
    private Boolean canManageSystem;

    // === EMPLOYEE BİLGİLERİ ===
    @Field("employee_id")
    private String employeeId; // Çalışan numarası

    @Field("hire_date")
    private LocalDateTime hireDate;

    @Field("supervisor_id")
    private String supervisorId; // Yönetici ID'si

    // === SATIŞ TEMSİLCİSİ ÖZELLİKLERİ ===

    @Field("spoken_languages")
    private Set<String> spokenLanguages; // "TR", "EN", "AR", "DE", "RU", "FR"

    @Field("is_available")
    private Boolean isAvailable; // Online/offline durumu

    @Field("max_concurrent_chats")
    @Min(value = 1, message = "En az 1 eş zamanlı chat olmalı")
    @Max(value = 20, message = "En fazla 20 eş zamanlı chat olabilir")
    private Integer maxConcurrentChats; // Eş zamanlı konuşma limiti

    @Field("current_active_chats")
    private Integer currentActiveChats; // Aktif konuşma sayısı

    @Field("assigned_lead_count")
    private Integer assignedLeadCount; // Toplam atanan lead

    @Field("converted_lead_count")
    private Integer convertedLeadCount; // Dönüştürülen lead

    @Field("average_response_time_seconds")
    private Long averageResponseTimeSeconds; // Ortalama yanıt süresi (saniye)

    @Field("specializations")
    private Set<String> specializations; // "HAIR_TRANSPLANT", "DENTAL", "EYE" vb.

    @Field("shift_start")
    private java.time.LocalTime shiftStart; // Mesai başlangıç

    @Field("shift_end")
    private java.time.LocalTime shiftEnd; // Mesai bitiş

    @Field("working_days")
    private Set<String> workingDays; // "MONDAY", "TUESDAY" vb.


    // === ACTIVITY TRACKING ===
    @Field("last_admin_action")
    private LocalDateTime lastAdminAction;

    @Field("total_actions_performed")
    private Integer totalActionsPerformed;

    @Field("users_managed")
    private Integer usersManaged;

    @Field("doctors_approved")
    private Integer doctorsApproved;

    @Field("clinics_approved")
    private Integer clinicsApproved;

    // === BUSINESS METHODS ===

    public boolean isSuperAdmin() {
        return AdminLevel.SUPER_ADMIN.equals(adminLevel);
    }

    public boolean canPerformAction(AdminPermission permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean canManageUserType(String userType) {
        return switch (userType.toUpperCase()) {
            case "PATIENT" -> canManageUsers;
            case "DOCTOR" -> canManageDoctors;
            case "CLINIC" -> canManageClinics;
            case "ADMIN" -> isSuperAdmin();
            default -> false;
        };
    }

    public String getAdminDisplayName() {
        return String.format("%s - %s (%s)", 
            getFullName(), 
            jobTitle != null ? jobTitle : "Admin", 
            department != null ? department : "Genel"
        );
    }

    public void recordAdminAction() {
        this.lastAdminAction = LocalDateTime.now();
        this.totalActionsPerformed = getTotalActionsPerformed() + 1;
    }

    public boolean isOnline() {
        return Boolean.TRUE.equals(isAvailable) && !isDeleted();
    }

    public boolean canAcceptNewChat() {
        return isOnline() && getCurrentActiveChats() < getMaxConcurrentChats();
    }

    public boolean speaksLanguage(String language) {
        return spokenLanguages != null && spokenLanguages.contains(language);
    }

    public boolean isSpecializedIn(String treatmentCategory) {
        return specializations != null && specializations.contains(treatmentCategory);
    }

    public double getLeadConversionRate() {
        if (getAssignedLeadCount() == 0) return 0.0;
        return (double) getConvertedLeadCount() / getAssignedLeadCount() * 100;
    }

    public void incrementActiveChats() {
        this.currentActiveChats = getCurrentActiveChats() + 1;
    }

    public void decrementActiveChats() {
        int current = getCurrentActiveChats();
        this.currentActiveChats = current > 0 ? current - 1 : 0;
    }

    public void recordLeadAssignment() {
        this.assignedLeadCount = getAssignedLeadCount() + 1;
    }

    public void recordLeadConversion() {
        this.convertedLeadCount = getConvertedLeadCount() + 1;
    }

    // === GETTER METHODS WITH DEFAULTS ===
    
    public AdminLevel getAdminLevel() {
        return adminLevel != null ? adminLevel : AdminLevel.STANDARD;
    }

    public Boolean getCanManageUsers() {
        return canManageUsers != null ? canManageUsers : true;
    }

    public Boolean getCanManageDoctors() {
        return Boolean.TRUE.equals(canManageDoctors) || canManageDoctors == null;
    }

    public Boolean getCanManageClinics() {
        return Boolean.TRUE.equals(canManageClinics);
    }

    public Boolean getCanViewReports() {
        return Boolean.TRUE.equals(canViewReports) || canViewReports == null;
    }

    public Boolean getCanManageSystem() {
        return Boolean.TRUE.equals(canManageSystem);
    }

    public Integer getTotalActionsPerformed() {
        return java.util.Objects.requireNonNullElse(totalActionsPerformed, 0);
    }

    public Integer getUsersManaged() {
        return java.util.Objects.requireNonNullElse(usersManaged, 0);
    }

    public Integer getDoctorsApproved() {
        return java.util.Objects.requireNonNullElse(doctorsApproved, 0);
    }

    public Integer getClinicsApproved() {
        return java.util.Objects.requireNonNullElse(clinicsApproved, 0);
    }
    public Boolean getIsAvailable() {
        return isAvailable != null ? isAvailable : false;
    }

    public Integer getMaxConcurrentChats() {
        return maxConcurrentChats != null ? maxConcurrentChats : 5;
    }

    public Integer getCurrentActiveChats() {
        return currentActiveChats != null ? currentActiveChats : 0;
    }

    public Integer getAssignedLeadCount() {
        return assignedLeadCount != null ? assignedLeadCount : 0;
    }

    public Integer getConvertedLeadCount() {
        return convertedLeadCount != null ? convertedLeadCount : 0;
    }

    public Long getAverageResponseTimeSeconds() {
        return averageResponseTimeSeconds != null ? averageResponseTimeSeconds : 0L;
    }

    // === NESTED ENUMS ===

    public enum AdminLevel {
        STANDARD("Standart Admin"),
        SENIOR("Kıdemli Admin"),
        MANAGER("Yönetici"),
        SUPER_ADMIN("Süper Admin");

        private final String displayName;

        AdminLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AdminPermission {
        // Kullanıcı yönetimi
        CREATE_USER("Kullanıcı Oluştur"),
        EDIT_USER("Kullanıcı Düzenle"),
        DELETE_USER("Kullanıcı Sil"),
        VIEW_USER_DETAILS("Kullanıcı Detayları Görüntüle"),

        // Doktor yönetimi
        APPROVE_DOCTOR("Doktor Onayla"),
        REJECT_DOCTOR("Doktor Reddet"),
        EDIT_DOCTOR("Doktor Düzenle"),
        VIEW_DOCTOR_REPORTS("Doktor Raporları Görüntüle"),

        // Klinik yönetimi
        APPROVE_CLINIC("Klinik Onayla"),
        REJECT_CLINIC("Klinik Reddet"),
        EDIT_CLINIC("Klinik Düzenle"),

        // Raporlar
        VIEW_FINANCIAL_REPORTS("Mali Raporlar"),
        VIEW_USER_STATISTICS("Kullanıcı İstatistikleri"),
        VIEW_APPOINTMENT_REPORTS("Randevu Raporları"),
        EXPORT_DATA("Veri Dışa Aktar"),

        // Sistem yönetimi
        MANAGE_SYSTEM_SETTINGS("Sistem Ayarları"),
        MANAGE_NOTIFICATIONS("Bildirim Yönetimi"),
        MANAGE_INTEGRATIONS("Entegrasyon Yönetimi"),
        ACCESS_AUDIT_LOGS("Audit Log Erişimi"),

        // Emergency
        EMERGENCY_ACCESS("Acil Durum Erişimi"),
        SYSTEM_MAINTENANCE("Sistem Bakımı");

        private final String displayName;

        AdminPermission(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}