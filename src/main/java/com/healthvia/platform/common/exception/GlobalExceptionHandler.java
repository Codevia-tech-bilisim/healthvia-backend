package com.healthvia.platform.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.healthvia.platform.appointment.exception.AppointmentExceptions;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // === BUSINESS EXCEPTION HANDLERS ===
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        log.error("Business exception: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .details(ex.getDetails())
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
    
    // === APPOINTMENT SPECIFIC EXCEPTION HANDLERS ===
    
    @ExceptionHandler(AppointmentExceptions.CancellationDeadlineException.class)
    public ResponseEntity<ApiResponse<Void>> handleCancellationDeadline(
            AppointmentExceptions.CancellationDeadlineException ex, WebRequest request) {
        
        log.error("Cancellation deadline exception: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .details(ex.getDetails())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(AppointmentExceptions.SlotNotAvailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleSlotNotAvailable(
            AppointmentExceptions.SlotNotAvailableException ex, WebRequest request) {
        
        log.error("Slot not available: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .details(ex.getDetails())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(AppointmentExceptions.SlotAlreadyBookedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSlotAlreadyBookedException(
        AppointmentExceptions.SlotAlreadyBookedException ex, WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .code("SLOT_ALREADY_BOOKED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(error));
    }

    @ExceptionHandler(AppointmentExceptions.PastDateAppointmentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePastDateAppointmentException(
            AppointmentExceptions.PastDateAppointmentException ex, WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .code("PAST_DATE_APPOINTMENT")
                .message("Geçmiş tarihler için randevu oluşturamazsınız")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error));
    }

    // === VALIDATION EXCEPTION HANDLERS ===
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation error: {}", errors);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Validasyon hatası")
            .details(errors)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
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
            .details(errors) 
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    // === SECURITY EXCEPTION HANDLERS ===
    
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
    
    // === DATA EXCEPTION HANDLERS ===
    
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
    
    // === GENERIC EXCEPTION HANDLER ===
    
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
}
