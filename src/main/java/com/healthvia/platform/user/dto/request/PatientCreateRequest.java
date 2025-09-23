package com.healthvia.platform.user.dto.request;

import java.time.LocalDate;

import com.healthvia.platform.common.validation.ValidTcKimlikNo;
import com.healthvia.platform.user.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreateRequest {
    
    @NotBlank(message = "Ad zorunludur")
    @Size(min = 2, max = 50)
    private String firstName;
    
    @NotBlank(message = "Soyad zorunludur")
    @Size(min = 2, max = 50)
    private String lastName;
    
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Geçerli email giriniz")
    private String email;
    
    @NotBlank(message = "Telefon zorunludur")
    private String phone;
    
    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 8)
    private String password;
    
    private User.Gender gender;
    private LocalDate birthDate;
    private String province;
    private String district;
    
    @ValidTcKimlikNo(allowNull = true)
    private String tcKimlikNo;
    
    private String passportNo;
    
    @NotBlank(message = "Doğum yeri zorunludur")
    private String birthPlace;
    
    @NotBlank(message = "Adres zorunludur")
    private String address;
    
    private String postalCode;
}