package com.healthvia.platform.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * TC Kimlik Numarası validation annotation'ı
 * Bean Validation ile entegre çalışır
 * 
 * Kullanım örneği:
 * @ValidTcKimlikNo
 * private String tcKimlikNo;
 * 
 * @ValidTcKimlikNo(allowNull = false)
 * private String tcKimlikNo;
 */
@Documented
@Constraint(validatedBy = TcKimlikNoConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTcKimlikNo {
    
    /**
     * Hata mesajı
     */
    String message() default "Geçersiz TC Kimlik Numarası";
    
    /**
     * Validation grupları
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Null değere izin verilip verilmeyeceği
     * true ise null değer valid kabul edilir
     * false ise null değer invalid kabul edilir
     */
    boolean allowNull() default true;
}