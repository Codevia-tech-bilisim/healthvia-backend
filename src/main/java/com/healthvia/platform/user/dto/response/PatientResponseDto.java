package com.healthvia.platform.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.healthvia.platform.common.util.TcKimlikNoValidator;
import com.healthvia.platform.user.entity.Patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDto {
    
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String maskedTcKimlikNo; // Maskelenmiş TC
    private LocalDate birthDate;
    private String gender;
    private String address;
    private String province;
    private String district;
    
    // Sağlık özeti
    private String bloodType;
    private Integer heightCm;
    private Double weightKg;
    private Double bmi;
    
    // Hesap bilgileri
    private String status;
    private Boolean emailVerified;
    private LocalDateTime lastLoginDate;
    
    // Factory method - Mevcut PatientDto stiline uygun
    public static PatientResponseDto fromEntity(Patient patient) {
        if (patient == null) return null;
        
        return PatientResponseDto.builder()
            .id(patient.getId())
            .firstName(patient.getFirstName())
            .lastName(patient.getLastName())
            .fullName(patient.getFirstName() + " " + patient.getLastName())
            .email(patient.getEmail())
            .phone(patient.getPhone())
            .maskedTcKimlikNo(patient.getTcKimlikNo() != null ? 
                TcKimlikNoValidator.mask(patient.getTcKimlikNo()) : null)
            .birthDate(patient.getBirthDate())
            .gender(patient.getGender() != null ? patient.getGender().name() : null)
            .address(patient.getAddress())
            .province(patient.getProvince())
            .district(patient.getDistrict())
            .bloodType(patient.getBloodType())
            .heightCm(patient.getHeightCm())
            .weightKg(patient.getWeightKg())
            .bmi(patient.getBMI())
            .status(patient.getStatus() != null ? patient.getStatus().name() : null)
            .emailVerified(patient.getEmailVerified())
            .lastLoginDate(patient.getLastLoginDate())
            .build();
    }
}