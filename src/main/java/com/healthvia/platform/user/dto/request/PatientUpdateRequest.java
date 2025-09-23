package com.healthvia.platform.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdateRequest {
    
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String province;
    private String district;
    private String postalCode;
    
    // Sağlık bilgileri
    private String allergies;
    private String chronicDiseases;
    private String currentMedications;
    private String familyMedicalHistory;
    
    @Pattern(regexp = "^(A|B|AB|0)[+-]$", message = "Geçersiz kan grubu")
    private String bloodType;
    
    @Min(50) @Max(250)
    private Integer heightCm;
    
    @DecimalMin("10.0") @DecimalMax("500.0")
    private Double weightKg;
    
    // Acil durum kişisi
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    
    // Sigorta
    private Boolean hasInsurance;
    private String insuranceCompany;
    private String insurancePolicyNumber;
}