// user/entity/User.java
package com.healthvia.platform.user.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.enums.Language;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.common.model.BaseEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Document(collection = "users")
public class User extends BaseEntity {

    // === TEMEL BİLGİLER ===
    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    @Field("first_name")
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    @Field("last_name")
    private String lastName;

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^[+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$", 
             message = "Geçerli bir telefon numarası giriniz")
    @Indexed(unique = true)
    private String phone;

    @NotBlank(message = "Şifre boş olamaz")
    private String password;

    // === KİŞİSEL BİLGİLER ===
    private Gender gender;

    @Field("birth_date")
    private LocalDate birthDate;

    // === KONUM BİLGİLERİ ===
    @Size(max = 50, message = "İl adı en fazla 50 karakter olabilir")
    private String province;

    @Size(max = 50, message = "İlçe adı en fazla 50 karakter olabilir") 
    private String district;

    // === SİSTEM BİLGİLERİ ===
    @NotNull(message = "Kullanıcı rolü belirtilmelidir")
    private UserRole role;

    @NotNull(message = "Kullanıcı durumu belirtilmelidir")
    private UserStatus status;

    @Field("email_verified")
    private Boolean emailVerified;

    @Field("phone_verified")
    private Boolean phoneVerified;

    @Field("profile_completion_rate")
    private Integer profileCompletionRate;

    @Field("last_login_date")
    private LocalDateTime lastLoginDate;

    @Field("failed_login_attempts")
    private Integer failedLoginAttempts;

    @Field("account_locked_until")
    private LocalDateTime accountLockedUntil;

    // === GDPR & CONSENT ===
    @Field("gdpr_consent")
    private Boolean gdprConsent;

    @Field("gdpr_consent_date")
    private LocalDateTime gdprConsentDate;

    @Field("marketing_consent")
    private Boolean marketingConsent;

    @Field("data_processing_consent")
    private Boolean dataProcessingConsent;

    @Field("data_sharing_consent")
    private Boolean dataSharingConsent;

    // === PREFERENCES ===
    @Field("preferred_language")
    private Language preferredLanguage;

    @Field("notification_preferences")
    private Set<String> notificationPreferences;

    @Field("avatar_url")
    private String avatarUrl;

    // === BUSINESS METHODS ===
    
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getAge() {
        if (birthDate == null) return 0;
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    public boolean isAdult() {
        return getAge() >= 18;
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isProfileComplete() {
        return profileCompletionRate != null && profileCompletionRate >= 80;
    }

    public Boolean getEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public Boolean getPhoneVerified() {
        return Boolean.TRUE.equals(phoneVerified);
    }

    public Integer getProfileCompletionRate() {
        return profileCompletionRate != null ? profileCompletionRate : 0;
    }

    public Integer getFailedLoginAttempts() {
        return java.util.Objects.requireNonNullElse(failedLoginAttempts, 0);
    }

    public Boolean getGdprConsent() {
        return Boolean.TRUE.equals(gdprConsent);
    }

    public Boolean getMarketingConsent() {
        return Boolean.TRUE.equals(marketingConsent);
    }

    public Boolean getDataProcessingConsent() {
        return Boolean.TRUE.equals(dataProcessingConsent);
    }

    public Boolean getDataSharingConsent() {
        return Boolean.TRUE.equals(dataSharingConsent);
    }

    public Language getPreferredLanguage() {
        return preferredLanguage != null ? preferredLanguage : Language.TURKISH;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = getFailedLoginAttempts() + 1;
        
        if (this.failedLoginAttempts >= 5) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void updateLastLogin() {
        this.lastLoginDate = LocalDateTime.now();
        resetFailedLoginAttempts();
    }

    // === ROLE CHECK METHODS ===
    
    public boolean isPatient() {
        return UserRole.PATIENT.equals(this.role);
    }

    public boolean isDoctor() {
        return UserRole.DOCTOR.equals(this.role);
    }

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    // === NESTED ENUMS ===
    
    public enum Gender {
        MALE("Erkek"),
        FEMALE("Kadın"),
        OTHER("Diğer"),
        PREFER_NOT_TO_SAY("Belirtmek İstemiyorum");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
    public void setProfileCompletionRate(Integer profileCompletionRate) {
    this.profileCompletionRate = profileCompletionRate;
}
}