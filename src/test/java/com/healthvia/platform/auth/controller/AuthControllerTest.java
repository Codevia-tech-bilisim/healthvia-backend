package com.healthvia.platform.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthvia.platform.auth.dto.AuthResponse;
import com.healthvia.platform.auth.dto.LoginRequest;
import com.healthvia.platform.auth.dto.RegisterRequest;
import com.healthvia.platform.auth.security.JwtAuthenticationFilter;
import com.healthvia.platform.auth.security.JwtTokenProvider;
import com.healthvia.platform.auth.service.AuthService;
import com.healthvia.platform.auth.service.CustomUserDetailsService;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.common.constants.ErrorCodes;

/**
 * AuthController icin unit testleri
 * Kimlik dogrulama, kayit ve token endpointleri test edilir
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {AuthController.class})
@DisplayName("AuthController Testleri")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AuthResponse successAuthResponse;

    @BeforeEach
    void setUp() {
        // Gecerli kayit istegi
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setFirstName("Ahmet");
        validRegisterRequest.setLastName("Yilmaz");
        validRegisterRequest.setEmail("ahmet@test.com");
        validRegisterRequest.setPhone("5551234567");
        validRegisterRequest.setPassword("Test123!@#");
        validRegisterRequest.setRole(UserRole.PATIENT);
        validRegisterRequest.setGdprConsent(true);
        validRegisterRequest.setBirthPlace("Istanbul");
        validRegisterRequest.setBirthDate(LocalDate.of(1990, 1, 15));

        // Gecerli giris istegi
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("ahmet@test.com");
        validLoginRequest.setPassword("Test123!@#");

        // Basarili auth response
        successAuthResponse = AuthResponse.builder()
            .accessToken("accessToken123")
            .refreshToken("refreshToken123")
            .tokenType("Bearer")
            .expiresIn(900000)
            .userId("user123")
            .email("ahmet@test.com")
            .firstName("Ahmet")
            .lastName("Yilmaz")
            .role(UserRole.PATIENT)
            .status(UserStatus.ACTIVE)
            .build();
    }

    @Nested
    @DisplayName("Hasta Kayit Endpoint Testleri")
    class HastaKayitEndpointTestleri {

        @Test
        @DisplayName("Basarili hasta kaydi")
        void registerPatient_Basarili() throws Exception {
            // Given
            given(authService.registerPatient(any(RegisterRequest.class)))
                .willReturn(successAuthResponse);

            // When & Then
            mockMvc.perform(post("/api/auth/register/patient")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken123"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refreshToken123"))
                    .andExpect(jsonPath("$.data.userId").value("user123"));

            verify(authService, times(1)).registerPatient(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Email zaten kayitli - hata")
        void registerPatient_EmailZatenKayitli() throws Exception {
            // Given
            given(authService.registerPatient(any(RegisterRequest.class)))
                .willThrow(new BusinessException(ErrorCodes.USER_ALREADY_EXISTS, "Email zaten kayitli"));

            // When & Then
            mockMvc.perform(post("/api/auth/register/patient")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Eksik zorunlu alan - validation hatasi")
        void registerPatient_EksikAlan() throws Exception {
            // Given - firstName bos
            validRegisterRequest.setFirstName("");

            // When & Then
            mockMvc.perform(post("/api/auth/register/patient")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Gecersiz email formati")
        void registerPatient_GecersizEmail() throws Exception {
            // Given
            validRegisterRequest.setEmail("gecersiz-email");

            // When & Then
            mockMvc.perform(post("/api/auth/register/patient")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Giris (Login) Endpoint Testleri")
    class GirisEndpointTestleri {

        @Test
        @DisplayName("Basarili giris")
        void login_Basarili() throws Exception {
            // Given
            given(authService.login(any(LoginRequest.class)))
                .willReturn(successAuthResponse);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.message").value("Giri\u015f ba\u015far\u0131l\u0131"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Yanlis sifre - hata")
        void login_YanlisSifre() throws Exception {
            // Given
            given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCodes.INVALID_CREDENTIALS));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Hesap kilitli - hata")
        void login_HesapKilitli() throws Exception {
            // Given
            given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCodes.ACCOUNT_LOCKED));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Bos kullanici adi - validation hatasi")
        void login_BosKullaniciAdi() throws Exception {
            // Given
            validLoginRequest.setUsername("");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Token Yenileme Endpoint Testleri")
    class TokenYenilemeEndpointTestleri {

        @Test
        @DisplayName("Basarili token yenileme")
        void refresh_Basarili() throws Exception {
            // Given
            String refreshToken = "validRefreshToken123";
            given(authService.refreshToken(anyString())).willReturn(successAuthResponse);

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists());
        }

        @Test
        @DisplayName("Gecersiz refresh token - hata")
        void refresh_GecersizToken() throws Exception {
            // Given
            String invalidToken = "invalidToken";
            given(authService.refreshToken(anyString()))
                .willThrow(new BusinessException(ErrorCodes.TOKEN_INVALID));

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidToken))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Cikis (Logout) Endpoint Testleri")
    class CikisEndpointTestleri {

        @Test
        @DisplayName("Basarili cikis")
        void logout_Basarili() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Email Dogrulama Endpoint Testleri")
    class EmailDogrulamaEndpointTestleri {

        @Test
        @DisplayName("Email dogrulama endpoint cagrisi")
        void verifyEmail() throws Exception {
            // Given
            String verificationToken = "verificationToken123";
            doNothing().when(authService).verifyEmail(anyString());

            // When & Then
            mockMvc.perform(post("/api/auth/verify-email")
                    .with(csrf())
                    .param("token", verificationToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Eksik token parametresi - hata")
        void verifyEmail_EksikToken() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/verify-email")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Sifre Sifirlama Endpoint Testleri")
    class SifreSifirlamaEndpointTestleri {

        @Test
        @DisplayName("Sifremi unuttum - basarili")
        void forgotPassword_Basarili() throws Exception {
            // Given
            String email = "ahmet@test.com";
            doNothing().when(authService).forgotPassword(anyString());

            // When & Then
            mockMvc.perform(post("/api/auth/forgot-password")
                    .with(csrf())
                    .param("email", email)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Sifre sifirlama - basarili")
        void resetPassword_Basarili() throws Exception {
            // Given
            String token = "resetToken123";
            String newPassword = "YeniSifre123!@#";
            doNothing().when(authService).resetPassword(anyString(), anyString());

            // When & Then
            mockMvc.perform(post("/api/auth/reset-password")
                    .with(csrf())
                    .param("token", token)
                    .param("newPassword", newPassword)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Sifre sifirlama - eksik parametre")
        void resetPassword_EksikParametre() throws Exception {
            // When & Then - token yok
            mockMvc.perform(post("/api/auth/reset-password")
                    .with(csrf())
                    .param("newPassword", "YeniSifre123!@#")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("JSON Parse Hata Testleri")
    class JsonParseHataTestleri {

        @Test
        @DisplayName("Gecersiz JSON formati")
        void login_GecersizJson() throws Exception {
            // Given - Gecersiz JSON
            String invalidJson = "{ gecersiz json }";

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Bos request body")
        void login_BosBody() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                    .andExpect(status().isBadRequest());
        }
    }
}
