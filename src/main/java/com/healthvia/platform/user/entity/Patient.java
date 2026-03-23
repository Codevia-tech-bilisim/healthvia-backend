// user/entity/Patient.java
package com.healthvia.platform.user.entity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.validation.ValidTcKimlikNo;

import jakarta.validation.constraints.NotBlank;
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
@Document(collection = "patients")
public class Patient extends User {

    // === KİMLİK BİLGİLERİ ===
    @ValidTcKimlikNo(allowNull = false, message = "Geçerli bir TC Kimlik No giriniz")
    @Indexed(unique = true, sparse = true)
    @Field("tc_kimlik_no")
    private String tcKimlikNo;

    @Size(max = 20, message = "Pasaport numarası en fazla 20 karakter olabilir")
    @Indexed(unique = true, sparse = true)
    @Field("passport_no")
    private String passportNo;

    @NotBlank(message = "Doğum yeri boş olamaz")
    @Size(max = 100, message = "Doğum yeri en fazla 100 karakter olabilir")
    @Field("birth_place")
    private String birthPlace;

    // === DETAYLI ADRES BİLGİLERİ ===
    @NotBlank(message = "Adres bilgisi boş olamaz")
    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String address;

    @Pattern(regexp = "^[0-9]{5}$", message = "Posta kodu 5 haneli olmalıdır")
    @Field("postal_code")
    private String postalCode;

    // === SAĞLIK BİLGİLERİ ===
    @Pattern(regexp = "^(A|B|AB|0)[+-]$", message = "Geçerli kan grubu giriniz (örn: A+, B-, AB+, 0-)")
    @Field("blood_type")
    private String bloodType;

    @Field("height_cm")
    private Integer heightCm; // Boy (cm)

    @Field("weight_kg")
    private Double weightKg; // Kilo (kg)

    @Size(max = 1000, message = "Alerji bilgisi en fazla 1000 karakter olabilir")
    private String allergies;

    @Size(max = 1000, message = "Kronik hastalık bilgisi en fazla 1000 karakter olabilir")
    @Field("chronic_diseases")
    private String chronicDiseases;

    @Size(max = 1000, message = "Kullandığı ilaçlar en fazla 1000 karakter olabilir")
    @Field("current_medications")
    private String currentMedications;

    @Size(max = 1000, message = "Ameliyat geçmişi en fazla 1000 karakter olabilir")
    @Field("surgery_history")
    private String surgeryHistory;

    @Size(max = 500, message = "Aile hastalık geçmişi en fazla 500 karakter olabilir")
    @Field("family_medical_history")
    private String familyMedicalHistory;

    // === ACIL DURUM İLETİŞİM ===
    @Size(max = 100, message = "Acil durum kişi adı en fazla 100 karakter olabilir")
    @Field("emergency_contact_name")
    private String emergencyContactName;

    @Pattern(regexp = "^[+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$", 
             message = "Geçerli bir acil durum telefon numarası giriniz")
    @Field("emergency_contact_phone")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Acil durum yakınlık derecesi en fazla 50 karakter olabilir")
    @Field("emergency_contact_relationship")
    private String emergencyContactRelationship; // anne, baba, eş, kardeş vs.

    // === SİGORTA BİLGİLERİ ===
    @Field("has_insurance")
    private boolean hasInsurance = false;

    @Size(max = 100, message = "Sigorta şirketi adı en fazla 100 karakter olabilir")
    @Field("insurance_company")
    private String insuranceCompany;

    @Size(max = 50, message = "Sigorta poliçe numarası en fazla 50 karakter olabilir")
    @Field("insurance_policy_number")
    private String insurancePolicyNumber;

    @Field("insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    // === HASTA TERCİHLERİ ===
    @Field("preferred_doctor_gender")
    private Gender preferredDoctorGender; // Tercih edilen doktor cinsiyeti

    @Field("smoking_status")
    private SmokingStatus smokingStatus;

    @Field("alcohol_consumption")
    private AlcoholConsumption alcoholConsumption;

    @Field("exercise_frequency")
    private ExerciseFrequency exerciseFrequency;

    // === İSTATİSTİKLER ===
    @Field("total_appointments")
    private Integer totalAppointments = 0;

    @Field("completed_appointments")
    private Integer completedAppointments = 0;

    @Field("cancelled_appointments")
    private Integer cancelledAppointments = 0;

    @Field("last_appointment_date")
    private LocalDate lastAppointmentDate;

    // === BUSINESS METHODS ===

    public boolean hasValidIdentity() {
        return (tcKimlikNo != null && !tcKimlikNo.trim().isEmpty()) || 
               (passportNo != null && !passportNo.trim().isEmpty());
    }

    public Double getBMI() {
        if (heightCm == null || weightKg == null || heightCm == 0) {
            return null;
        }
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    public String getBMICategory() {
        Double bmi = getBMI();
        if (bmi == null) return "Bilinmiyor";
        
        if (bmi < 18.5) return "Zayıf";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Fazla Kilolu";
        return "Obez";
    }

    public boolean hasHealthIssues() {
        return (allergies != null && !allergies.trim().isEmpty()) ||
               (chronicDiseases != null && !chronicDiseases.trim().isEmpty()) ||
               (currentMedications != null && !currentMedications.trim().isEmpty());
    }

    public double getAppointmentCompletionRate() {
        if (totalAppointments == null || totalAppointments == 0) return 0.0;
        return (completedAppointments != null ? completedAppointments : 0) * 100.0 / totalAppointments;
    }

    /**
     * Calculates profile completion rate based on filled fields.
     * Fields and their weights:
     * - Basic info (firstName, lastName, email, phone, birthDate, gender): 30% (already filled at registration)
     * - Identity (tcKimlikNo or passportNo, birthPlace, address): 10%
     * - Health info (bloodType, heightCm, weightKg): 15%
     * - Medical history (allergies, chronicDiseases, currentMedications, familyMedicalHistory): 15%
     * - Lifestyle (smokingStatus, alcoholConsumption, exerciseFrequency): 10%
     * - Emergency contact (emergencyContactName, emergencyContactPhone, emergencyContactRelationship): 10%
     * - Insurance (hasInsurance + details): 10%
     */
    public int calculateProfileCompletionRate() {
        int rate = 0;

        // Basic info — 30%
        int basicCount = 0;
        if (getFirstName() != null && !getFirstName().isBlank()) basicCount++;
        if (getLastName() != null && !getLastName().isBlank()) basicCount++;
        if (getEmail() != null && !getEmail().isBlank()) basicCount++;
        if (getPhone() != null && !getPhone().isBlank()) basicCount++;
        if (getBirthDate() != null) basicCount++;
        if (getGender() != null) basicCount++;
        rate += (int) Math.round(30.0 * basicCount / 6);

        // Identity & address — 10%
        int identityCount = 0;
        if ((tcKimlikNo != null && !tcKimlikNo.isBlank()) || (passportNo != null && !passportNo.isBlank())) identityCount++;
        if (birthPlace != null && !birthPlace.isBlank()) identityCount++;
        if (address != null && !address.isBlank()) identityCount++;
        rate += (int) Math.round(10.0 * identityCount / 3);

        // Health info — 15%
        int healthCount = 0;
        if (bloodType != null && !bloodType.isBlank()) healthCount++;
        if (heightCm != null) healthCount++;
        if (weightKg != null) healthCount++;
        rate += (int) Math.round(15.0 * healthCount / 3);

        // Medical history — 15%
        int medicalCount = 0;
        if (allergies != null && !allergies.isBlank()) medicalCount++;
        if (chronicDiseases != null && !chronicDiseases.isBlank()) medicalCount++;
        if (currentMedications != null && !currentMedications.isBlank()) medicalCount++;
        if (familyMedicalHistory != null && !familyMedicalHistory.isBlank()) medicalCount++;
        rate += (int) Math.round(15.0 * medicalCount / 4);

        // Lifestyle — 10%
        int lifestyleCount = 0;
        if (smokingStatus != null) lifestyleCount++;
        if (alcoholConsumption != null) lifestyleCount++;
        if (exerciseFrequency != null) lifestyleCount++;
        rate += (int) Math.round(10.0 * lifestyleCount / 3);

        // Emergency contact — 10%
        int emergencyCount = 0;
        if (emergencyContactName != null && !emergencyContactName.isBlank()) emergencyCount++;
        if (emergencyContactPhone != null && !emergencyContactPhone.isBlank()) emergencyCount++;
        if (emergencyContactRelationship != null && !emergencyContactRelationship.isBlank()) emergencyCount++;
        rate += (int) Math.round(10.0 * emergencyCount / 3);

        return Math.min(rate, 100);
    }

    // === NESTED ENUMS ===

    public enum SmokingStatus {
        NEVER("Hiç İçmedi"),
        FORMER("Bıraktı"),
        OCCASIONAL("Ara Sıra"),
        REGULAR("Düzenli İçiyor");

        private final String displayName;
        SmokingStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum AlcoholConsumption {
        NEVER("Hiç İçmiyor"),
        RARELY("Nadiren"),
        OCCASIONALLY("Ara Sıra"),
        REGULARLY("Düzenli");

        private final String displayName;
        AlcoholConsumption(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ExerciseFrequency {
        NONE("Hiç"),
        RARELY("Nadiren"),
        ONE_TWO_TIMES("Haftada 1-2"),
        THREE_FOUR_TIMES("Haftada 3-4"),
        DAILY("Her Gün");

        private final String displayName;
        ExerciseFrequency(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}