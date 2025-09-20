package com.healthvia.platform.common.validation;

import com.healthvia.platform.common.util.TcKimlikNoValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * TC Kimlik No validation logic implementation
 * ValidTcKimlikNo annotation'ı ile birlikte çalışır
 */
@Slf4j
public class TcKimlikNoConstraintValidator implements ConstraintValidator<ValidTcKimlikNo, String> {
    
    private boolean allowNull;
    
    @Override
    public void initialize(ValidTcKimlikNo constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        log.debug("TcKimlikNoConstraintValidator initialized with allowNull: {}", allowNull);
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null değer kontrolü
        if (value == null) {
            return allowNull;
        }
        
        // Boş string kontrolü
        if (value.trim().isEmpty()) {
            return false;
        }
        
        // TC Kimlik No algoritma kontrolü
        boolean isValid = TcKimlikNoValidator.isValid(value);
        
        if (!isValid) {
            log.debug("Invalid TC Kimlik No: {}", TcKimlikNoValidator.mask(value));
        }
        
        return isValid;
    }
}