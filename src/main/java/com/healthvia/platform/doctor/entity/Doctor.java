// user/entity/Doctor.java
package com.healthvia.platform.doctor.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.user.entity.User;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@Document(collection = "doctors")
public class Doctor extends User {

    // === MESLEKİ KİMLİK ===
    @NotBlank(message = "Diploma numarası boş olamaz")
    @Indexed(unique = true)
    @Field("diploma_number")
    private String diplomaNumber;

    @NotBlank(message = "Tabip Odası sicil numarası boş olamaz")
    @Indexed(unique = true)
    @Field("medical_license_number")
    private String medicalLicenseNumber;

    // === EĞİTİM BİLGİLERİ ===
    @NotBlank(message = "Mezun olduğu tıp fakültesi boş olamaz")
    @Size(max = 200, message = "Tıp fakültesi adı en fazla 200 karakter olabilir")
    @Field("medical_school")
    private String medicalSchool;

    @Field("graduation_year")
    @Min(value = 1950, message = "Mezuniyet yılı 1950'den önce olamaz")
    @Max(value = 2030, message = "Mezuniyet yılı 2030'dan sonra olamaz")
    private Integer graduationYear;

    @Size(max = 200, message = "Uzmanlık alanı en fazla 200 karakter olabilir")
    @Field("specialty_school")
    private String specialtySchool; // Uzmanlık yaptığı fakülte

    @Field("specialty_completion_year")
    private Integer specialtyCompletionYear;

    // === UZMANLIK ALANLARI ===
    @Field("primary_specialty")
    private String primarySpecialty; // Ana uzmanlık alanı

    @Field("subspecialties") 
    private Set<String> subspecialties; // Yan dal uzmanlıkları

    @Field("medical_interests")
    private Set<String> medicalInterests; // İlgi alanları

    // === PROFESYONEL DENEYİM ===
    @Field("years_of_experience")
    @Min(value = 0, message = "Deneyim yılı negatif olamaz")
    private Integer yearsOfExperience;

    @Field("current_hospital")
    private String currentHospital; // Şu an çalıştığı hastane

    @Field("current_clinic")
    private String currentClinic; // Şu an çalıştığı klinik

    @Field("job_title")
    private String jobTitle; // Başhekim, Doktor, Uzman Dr. vs.

    // === RANDEVU & ÇALIŞMA SAATLERİ ===
    @Field("consultation_fee")
    @DecimalMin(value = "0.0", message = "Muayene ücreti negatif olamaz")
    @DecimalMax(value = "10000.0", message = "Muayene ücreti çok yüksek")
    private BigDecimal consultationFee;

    @Field("consultation_duration_minutes")
    @Min(value = 15, message = "Muayene süresi en az 15 dakika olmalı")
    @Max(value = 120, message = "Muayene süresi en fazla 120 dakika olabilir")
    private Integer consultationDurationMinutes;

    @Field("working_days")
    private Set<String> workingDays; // MONDAY, TUESDAY vs.

    @Field("working_hours_start")
    private LocalTime workingHoursStart;

    @Field("working_hours_end") 
    private LocalTime workingHoursEnd;

    @Field("lunch_break_start")
    private LocalTime lunchBreakStart;

    @Field("lunch_break_end")
    private LocalTime lunchBreakEnd;

    @Field("appointment_buffer_minutes")
    private Integer appointmentBufferMinutes; // Randevular arası boşluk

    // === BIYOGRAFI & CV ===
    @Size(max = 2000, message = "Biyografi en fazla 2000 karakter olabilir")
    private String biography;

    @Size(max = 5000, message = "CV en fazla 5000 karakter olabilir")
    private String curriculum;

    @Field("awards_and_recognitions")
    private List<String> awardsAndRecognitions;

    @Field("publications")
    private List<String> publications; // Yayınları

    @Field("conferences_attended")
    private List<String> conferencesAttended;

    // === SERTİFİKALAR VE LİSANSLAR ===
    @Field("certifications")
    private List<Certification> certifications;

    @Field("professional_memberships")
    private Set<String> professionalMemberships; // TTB, Uzman Dernekler

    // === HİZMET VERDİĞİ DURUMLAR ===
    @Field("treatable_conditions")
    private Set<String> treatableConditions; // Tedavi ettiği hastalıklar

    @Field("offered_services")
    private Set<String> offeredServices; // Sunduğu hizmetler

    @Field("languages_spoken")
    private Set<String> languagesSpoken; // Konuştuğu diller

    // === RANDEVU İSTATİSTİKLERİ ===
    @Field("total_appointments")
    private Integer totalAppointments;

    @Field("completed_appointments")
    private Integer completedAppointments;

    @Field("cancelled_appointments")
    private Integer cancelledAppointments;

    @Field("average_rating")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private Double averageRating;

    @Field("total_reviews")
    private Integer totalReviews;

    @Field("patients_treated")
    private Integer patientsTreated;

    // === DURUMLAR ===
    @Field("is_accepting_new_patients")
    private Boolean isAcceptingNewPatients;

    @Field("is_available_for_emergencies")
    private Boolean isAvailableForEmergencies;

    @Field("consultation_types")
    private Set<ConsultationType> consultationTypes; // Online, Yüz yüze vs.

    @Field("verification_status")
    private VerificationStatus verificationStatus;

    @Field("verification_date")
    private LocalDate verificationDate;

    // === BUSINESS METHODS ===

    public boolean isVerified() {
        return VerificationStatus.VERIFIED.equals(verificationStatus);
    }

    public boolean isExperienced() {
        return yearsOfExperience != null && yearsOfExperience >= 5;
    }

    public boolean hasHighRating() {
        return averageRating != null && averageRating >= 4.0;
    }

    public double getAppointmentCompletionRate() {
        Integer total = getTotalAppointments();
        if (total == 0) return 0.0;
        return getCompletedAppointments() * 100.0 / total;
    }

 
    public boolean isAvailableOnDay(String dayOfWeek) {
        return workingDays != null && workingDays.contains(dayOfWeek.toUpperCase());
    }

    // === GETTER METHODS WITH DEFAULTS ===
    
    public Integer getConsultationDurationMinutes() {
        return consultationDurationMinutes != null ? consultationDurationMinutes : 30;
    }

    public Integer getAppointmentBufferMinutes() {
        return appointmentBufferMinutes != null ? appointmentBufferMinutes : 5;
    }

    public Integer getTotalAppointments() {
        return totalAppointments != null ? totalAppointments : 0;
    }

    public Integer getCompletedAppointments() {
        return completedAppointments != null ? completedAppointments : 0;
    }

    public Integer getCancelledAppointments() {
        return cancelledAppointments != null ? cancelledAppointments : 0;
    }

    public Double getAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }

    public Integer getTotalReviews() {
        return totalReviews != null ? totalReviews : 0;
    }

    public Integer getPatientsTreated() {
        return patientsTreated != null ? patientsTreated : 0;
    }

    public Boolean getIsAcceptingNewPatients() {
        return isAcceptingNewPatients != null ? isAcceptingNewPatients : true;
    }

    public Boolean getIsAvailableForEmergencies() {
        return isAvailableForEmergencies != null ? isAvailableForEmergencies : false;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
    }

    // === NESTED CLASSES & ENUMS ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certification {
        private String name;
        private String issuer;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private String certificateNumber;
        private boolean isValid;

        public boolean isExpired() {
            return expiryDate != null && expiryDate.isBefore(LocalDate.now());
        }
    }

    public enum ConsultationType {
        IN_PERSON("Yüz Yüze"),
        ONLINE("Online"),
        HOME_VISIT("Ev Ziyareti"),
        PHONE_CONSULTATION("Telefon Konsültasyonu");

        private final String displayName;
        ConsultationType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum VerificationStatus {
        PENDING("Onay Bekliyor"),
        VERIFIED("Onaylandı"),
        REJECTED("Reddedildi"),
        SUSPENDED("Askıya Alındı");

        private final String displayName;
        VerificationStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}