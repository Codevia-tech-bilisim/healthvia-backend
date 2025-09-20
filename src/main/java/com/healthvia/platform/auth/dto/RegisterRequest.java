// auth/dto/RegisterRequest.java
package com.healthvia.platform.auth.dto;

import java.time.LocalDate;

import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.validation.ValidTcKimlikNo;
import com.healthvia.platform.user.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    private String firstName;
    
    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    private String lastName;
    
    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;
    
    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^[+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$", 
             message = "Geçerli bir telefon numarası giriniz")
    private String phone;
    
    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.!]).*$", 
             message = "Şifre büyük harf, küçük harf, rakam ve özel karakter içermelidir")
    private String password;
    
    @NotNull(message = "Kullanıcı tipi belirtilmelidir")
    private UserRole role;
    
    private User.Gender gender;
    private LocalDate birthDate;
    private String province;
    private String district;
    
    @NotNull(message = "GDPR onayı gereklidir")
    private Boolean gdprConsent;
    
    // Patient için ek alanlar
    @ValidTcKimlikNo(allowNull = true, message = "Geçerli bir TC Kimlik No giriniz")
    private String tcKimlikNo;
    private String passportNo;
    @NotBlank(message = "Doğum yeri boş olamaz")
    private String birthPlace;
}
