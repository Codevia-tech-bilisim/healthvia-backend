// common/exception/GlobalExceptionHandler.java
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

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.dto.ErrorResponse;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // ===== HELPER METHODS =====
    
    /**
     * HTTP Request'ten Correlation ID'yi alır
     */
    private String getCorrelationId(HttpServletRequest request) {
        return request != null ? request.getHeader("X-Correlation-ID") : null;
    }
    
    /**
     * Proxy/Load balancer arkasındaki gerçek client IP'sini alır
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Request path'ini temizler
     */
    private String getCleanPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    
    // ===== BUSINESS EXCEPTION HANDLERS =====
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.error("Business exception - CorrelationId: {}, IP: {}, Error: {}", 
                correlationId, clientIp, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(getCleanPath(request))
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.error("Resource not found - CorrelationId: {}, IP: {}, Resource: {}", 
                correlationId, clientIp, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("RESOURCE_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(getCleanPath(request))
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error));
    }

    // ===== VALIDATION EXCEPTION HANDLERS =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        Map<String, Object> errors = new HashMap<>(); // ✅ Map<String, Object> olarak değiştirin
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage); // String değeri Object'e cast olur
        });
        
        log.error("Validation error - CorrelationId: {}, IP: {}, Errors: {}", 
                correlationId, clientIp, errors);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Validation hatası")
            .details(errors) // ✅ Artık tip uyumlu
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        Map<String, Object> errors = new HashMap<>(); // ✅ Map<String, Object> olarak değiştirin
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message); // String değeri Object'e cast olur
        });
        
        log.error("Constraint violation - CorrelationId: {}, Errors: {}", correlationId, errors);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("CONSTRAINT_VIOLATION")
            .message("Kısıtlama ihlali")
            .details(errors) // ✅ Artık tip uyumlu
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    // ===== SECURITY EXCEPTION HANDLERS =====
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";
        
        // GÜVENLİK: Kötü amaçlı login denemelerini loglama
        log.warn("Failed login attempt - CorrelationId: {}, IP: {}, UserAgent: {}", 
                correlationId, clientIp, userAgent);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INVALID_CREDENTIALS")
            .message("Geçersiz kullanıcı adı veya şifre")
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        String requestUri = httpRequest != null ? httpRequest.getRequestURI() : "unknown";
        String method = httpRequest != null ? httpRequest.getMethod() : "unknown";
        
        // GÜVENLİK: Yetkisiz erişim denemelerini loglama
        log.warn("Access denied - CorrelationId: {}, IP: {}, URI: {}, Method: {}", 
                correlationId, clientIp, requestUri, method);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("ACCESS_DENIED")
            .message("Bu işlem için yetkiniz bulunmamaktadır")
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.error("Authentication error - CorrelationId: {}, IP: {}, Error: {}", 
                correlationId, clientIp, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("AUTHENTICATION_ERROR")
            .message("Kimlik doğrulama hatası")
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error));
    }
    
    /**
     * JWT Token ile ilgili özel exception handler
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(
            JwtException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.warn("JWT token error - CorrelationId: {}, IP: {}, Error: {}", 
                correlationId, clientIp, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INVALID_TOKEN")
            .message("Geçersiz veya süresi dolmuş token")
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error));
    }

    // ===== HTTP EXCEPTION HANDLERS =====

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.error("JSON parse error - CorrelationId: {}, IP: {}, Error: {}", 
                correlationId, clientIp, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INVALID_JSON")
            .message("Geçersiz JSON formatı veya veri tipi hatası")
            .timestamp(LocalDateTime.now())
            .path(getCleanPath(request))
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(
            MissingServletRequestParameterException ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("MISSING_PARAMETER")
            .message("Eksik parametre: " + ex.getParameterName())
            .timestamp(LocalDateTime.now())
            .path(getCleanPath(request))
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.error("File size exceeded - CorrelationId: {}, IP: {}, MaxSize: {}", 
                correlationId, clientIp, ex.getMaxUploadSize());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("FILE_SIZE_EXCEEDED")
            .message("Dosya boyutu çok büyük. Maksimum 10MB yükleyebilirsiniz.")
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.error("Data integrity violation - CorrelationId: {}, IP: {}, Error: {}", 
                correlationId, clientIp, ex.getMessage());
        
        String message = "Veri bütünlüğü hatası";
        if (ex.getMessage().contains("duplicate key")) {
            message = "Bu kayıt zaten mevcut";
        }
        
        ErrorResponse error = ErrorResponse.builder()
            .code("DATA_INTEGRITY_ERROR")
            .message(message)
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(error));
    }

    // ===== APPOINTMENT SPECIFIC EXCEPTIONS =====

    @ExceptionHandler(AppointmentExceptions.SlotAlreadyBookedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSlotAlreadyBookedException(
            AppointmentExceptions.SlotAlreadyBookedException ex, WebRequest request, HttpServletRequest httpRequest) {

        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.warn("Slot booking conflict - CorrelationId: {}, IP: {}, Message: {}", 
                correlationId, clientIp, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("SLOT_ALREADY_BOOKED")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(getCleanPath(request))
                .correlationId(correlationId)
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(ApiResponse.error(error));
    }

    @ExceptionHandler(AppointmentExceptions.PastDateAppointmentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePastDateAppointmentException(
            AppointmentExceptions.PastDateAppointmentException ex, WebRequest request, HttpServletRequest httpRequest) {

        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        
        log.warn("Past date appointment attempt - CorrelationId: {}, IP: {}, Message: {}", 
                correlationId, clientIp, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("PAST_DATE_APPOINTMENT")
                .message("Geçmiş tarihler için randevu oluşturamazsınız")
                .timestamp(LocalDateTime.now())
                .path(getCleanPath(request))
                .correlationId(correlationId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error(error));
    }
    
    // ===== RATE LIMITING & PERFORMANCE =====
    
    /**
     * Rate Limiting için özel exception handler
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";
        
        // GÜVENLİK: Rate limit aşımlarını loglama
        log.warn("Rate limit exceeded - CorrelationId: {}, IP: {}, UserAgent: {}, Limit: {}", 
                correlationId, clientIp, userAgent, ex.getLimit());
        
        ErrorResponse error = ErrorResponse.builder()
            .code("RATE_LIMIT_EXCEEDED")
            .message("Çok fazla istek gönderdiniz. Lütfen biraz bekleyip tekrar deneyiniz.")
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .details(Map.of(
                "retryAfter", ex.getRetryAfterSeconds() + " seconds",
                "limit", ex.getLimit(),
                "window", ex.getWindowMinutes() + " minutes"
            ))
            .build();
        
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS) // 429
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(ApiResponse.error(error));
    }
    
    // ===== GLOBAL EXCEPTION HANDLER =====
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request, HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        String clientIp = getClientIp(httpRequest);
        String requestUri = httpRequest != null ? httpRequest.getRequestURI() : "unknown";
        String method = httpRequest != null ? httpRequest.getMethod() : "unknown";
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";
        
        // KRITIK: Beklenmeyen hataları detaylı loglama
        log.error("Unexpected error - CorrelationId: {}, IP: {}, URI: {}, Method: {}, UserAgent: {}", 
                correlationId, clientIp, requestUri, method, userAgent, ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("Beklenmeyen bir hata oluştu")
            .timestamp(LocalDateTime.now())
            .path(getCleanPath(request))
            .correlationId(correlationId)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error));
    }
}

// ===== CUSTOM EXCEPTIONS =====

/**
 * Rate limiting için custom exception
 */
class RateLimitExceededException extends RuntimeException {
    private final int limit;
    private final int windowMinutes;
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(int limit, int windowMinutes, long retryAfterSeconds) {
        super("Rate limit exceeded: " + limit + " requests per " + windowMinutes + " minutes");
        this.limit = limit;
        this.windowMinutes = windowMinutes;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getLimit() { return limit; }
    public int getWindowMinutes() { return windowMinutes; }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}