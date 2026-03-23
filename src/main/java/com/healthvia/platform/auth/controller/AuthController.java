package com.healthvia.platform.auth.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.healthvia.platform.auth.dto.AuthResponse;
import com.healthvia.platform.auth.dto.LoginRequest;
import com.healthvia.platform.auth.dto.RegisterAdminRequest;
import com.healthvia.platform.auth.dto.RegisterDoctorRequest;
import com.healthvia.platform.auth.dto.RegisterRequest;
import com.healthvia.platform.auth.service.AuthService;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.dto.ErrorResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // === ROLE-SPECIFIC REGISTRATIONS ===
    
    @PostMapping("/register/patient")
    public ApiResponse<AuthResponse> registerPatient(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerPatient(request);
        return ApiResponse.success(response, "Hasta kaydı başarılı. Email doğrulaması için mailinizi kontrol edin.");
    }

    @PostMapping("/register/doctor")
    public ApiResponse<AuthResponse> registerDoctor(@Valid @RequestBody RegisterDoctorRequest request) {
        AuthResponse response = authService.registerDoctor(request);
        return ApiResponse.success(response, "Doktor kaydı başarılı. Hesabınız inceleme altındadır.");
    }

    @PostMapping("/register/admin")
    public ApiResponse<AuthResponse> registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
        AuthResponse response = authService.registerAdmin(request);
        return ApiResponse.success(response, "Admin kaydı başarılı.");
    }

    // === AUTHENTICATION ===
    
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(response, "Giriş başarılı");
    }

    // === TOKEN OPERATIONS ===
    
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ApiResponse.success(response, "Token yenilendi");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT stateless olduğu için client-side logout
        return ApiResponse.success("Çıkış başarılı");
    }

    // === EMAIL & PASSWORD OPERATIONS ===
    
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.success("Email doğrulandı");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email adresi gereklidir");
        }
        authService.forgotPassword(email);
        return ApiResponse.success("Şifre sıfırlama bağlantısı email adresinize gönderildi");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody java.util.Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Token ve yeni şifre gereklidir");
        }
        authService.resetPassword(token, newPassword);
        return ApiResponse.success("Şifre başarıyla güncellendi");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.error("JSON parse error: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .code("INVALID_JSON")
            .message("Geçersiz JSON formatı veya veri tipi hatası")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(
            MissingServletRequestParameterException ex, WebRequest request) {
        ErrorResponse error = ErrorResponse.builder()
            .code("MISSING_PARAMETER")
            .message("Eksik parametre: " + ex.getParameterName())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }
}