// common/exception/GlobalExceptionHandler.java
package com.healthvia.platform.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.healthvia.platform.appointment.exception.AppointmentExceptions;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        log.error("Business exception: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        log.error("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("RESOURCE_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation error: {}", errors);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Validation hatası")
            .details(errors)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex) {
        
        log.error("Bad credentials: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INVALID_CREDENTIALS")
            .message("Geçersiz kullanıcı adı veya şifre")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex) {
        
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("ACCESS_DENIED")
            .message("Bu işlem için yetkiniz bulunmamaktadır")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex) {
        
        log.error("Authentication error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("AUTHENTICATION_ERROR")
            .message("Kimlik doğrulama hatası")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        
        log.error("File size exceeded: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("FILE_SIZE_EXCEEDED")
            .message("Dosya boyutu çok büyük. Maksimum 10MB yükleyebilirsiniz.")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        
        log.error("Data integrity violation: {}", ex.getMessage());
        
        String message = "Veri bütünlüğü hatası";
        if (ex.getMessage().contains("duplicate key")) {
            message = "Bu kayıt zaten mevcut";
        }
        
        ErrorResponse error = ErrorResponse.builder()
            .code("DATA_INTEGRITY_ERROR")
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
        ConstraintViolationException ex) {
    
    Map<String, String> errors = new HashMap<>();
    ex.getConstraintViolations().forEach(violation -> {
        String propertyPath = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        errors.put(propertyPath, message);
    });
    
    log.error("Constraint violation: {}", errors);
    
    ErrorResponse error = ErrorResponse.builder()
        .code("CONSTRAINT_VIOLATION")
        .message("Kısıtlama ihlali")
        .details(errors) // <-- Burada String değil Map gönderiyoruz
        .timestamp(LocalDateTime.now())
        .build();
    
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(error));
}
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error: ", ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("Beklenmeyen bir hata oluştu")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error));
    }

//    @ExceptionHandler(AppointmentExceptions.CancellationDeadlineException.class)
//    public ResponseEntity<ErrorResponse> handleCancellationDeadline(
//            AppointmentExceptions.CancellationDeadlineException ex) {
//
//        ErrorResponse response = ErrorResponse.builder()
//                .code(ex.getErrorCode().name())
//                .message(ex.getDetails().get("message").toString())  // özel mesajı alıyoruz
//                .details(ex.getDetails().toString())
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
//    }
}
