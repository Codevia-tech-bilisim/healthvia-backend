package com.healthvia.platform.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.auth.dto.AuthResponse;
import com.healthvia.platform.auth.dto.LoginRequest;
import com.healthvia.platform.auth.dto.RegisterRequest;
import com.healthvia.platform.auth.security.JwtTokenProvider;
import com.healthvia.platform.auth.security.UserPrincipal;
import com.healthvia.platform.auth.service.impl.AuthServiceImpl;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.doctor.entity.Doctor;
import com.healthvia.platform.doctor.repository.DoctorRepository;
import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.repository.PatientRepository;
import com.healthvia.platform.user.repository.UserRepository;

/**
 * AuthServiceImpl icin unit testleri
 * Kimlik dogrulama, kayit ve token islemlerini test eder
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Testleri")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        // Gecerli kayit istegi olustur
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
        validRegisterRequest.setProvince("Istanbul");
        validRegisterRequest.setDistrict("Kadikoy");

        // Gecerli giris istegi olustur
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("ahmet@test.com");
        validLoginRequest.setPassword("Test123!@#");

        // Test hastasi olustur
        testPatient = Patient.builder()
            .firstName("Ahmet")
            .lastName("Yilmaz")
            .email("ahmet@test.com")
            .phone("5551234567")
            .password("encodedPassword123")
            .role(UserRole.PATIENT)
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .phoneVerified(false)
            .failedLoginAttempts(0)
            .build();
        testPatient.setId("patient123");
    }

    @Nested
    @DisplayName("Hasta Kayit Testleri")
    class HastaKayitTestleri {

        @Test
        @DisplayName("Basarili hasta kaydi - tum alanlar gecerli")
        void registerPatient_BasariliKayit() {
            // Given
            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByEmail(anyString())).willReturn(false);
            given(doctorRepository.existsByEmail(anyString())).willReturn(false);
            given(adminRepository.existsByEmail(anyString())).willReturn(false);

            given(userRepository.existsByPhoneAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByPhone(anyString())).willReturn(false);
            given(doctorRepository.existsByPhone(anyString())).willReturn(false);
            given(adminRepository.existsByPhone(anyString())).willReturn(false);

            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(patientRepository.save(any(Patient.class))).willReturn(testPatient);
            given(tokenProvider.generateAccessToken(any(UserPrincipal.class))).willReturn("accessToken123");
            given(tokenProvider.generateRefreshToken(anyString())).willReturn("refreshToken123");

            // When
            AuthResponse response = authService.registerPatient(validRegisterRequest);

            // Then
            assertNotNull(response);
            assertEquals("accessToken123", response.getAccessToken());
            assertEquals("refreshToken123", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertFalse(response.getRequiresAction());

            verify(patientRepository, times(1)).save(any(Patient.class));
            verify(passwordEncoder, times(1)).encode(validRegisterRequest.getPassword());
        }

        @Test
        @DisplayName("Email zaten kayitli - hata firlatmali")
        void registerPatient_EmailZatenKayitli() {
            // Given
            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(true);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.registerPatient(validRegisterRequest);
            });

            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("Telefon zaten kayitli - hata firlatmali")
        void registerPatient_TelefonZatenKayitli() {
            // Given
            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByEmail(anyString())).willReturn(false);
            given(doctorRepository.existsByEmail(anyString())).willReturn(false);
            given(adminRepository.existsByEmail(anyString())).willReturn(false);

            given(userRepository.existsByPhoneAndDeletedFalse(anyString())).willReturn(true);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.registerPatient(validRegisterRequest);
            });

            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("GDPR onayi verilmemis - hata firlatmali")
        void registerPatient_GdprOnayiYok() {
            // Given
            validRegisterRequest.setGdprConsent(false);

            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByEmail(anyString())).willReturn(false);
            given(doctorRepository.existsByEmail(anyString())).willReturn(false);
            given(adminRepository.existsByEmail(anyString())).willReturn(false);

            given(userRepository.existsByPhoneAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByPhone(anyString())).willReturn(false);
            given(doctorRepository.existsByPhone(anyString())).willReturn(false);
            given(adminRepository.existsByPhone(anyString())).willReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.registerPatient(validRegisterRequest);
            });

            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("Gecerli TC Kimlik No ile kayit")
        void registerPatient_GecerliTcKimlikNo() {
            // Given - Gecerli bir TC Kimlik No (algoritma kontrol edilmeli)
            validRegisterRequest.setTcKimlikNo("10000000146"); // Ornek gecerli TC

            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByEmail(anyString())).willReturn(false);
            given(doctorRepository.existsByEmail(anyString())).willReturn(false);
            given(adminRepository.existsByEmail(anyString())).willReturn(false);

            given(userRepository.existsByPhoneAndDeletedFalse(anyString())).willReturn(false);
            given(patientRepository.existsByPhone(anyString())).willReturn(false);
            given(doctorRepository.existsByPhone(anyString())).willReturn(false);
            given(adminRepository.existsByPhone(anyString())).willReturn(false);

            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(patientRepository.save(any(Patient.class))).willReturn(testPatient);
            given(tokenProvider.generateAccessToken(any(UserPrincipal.class))).willReturn("accessToken123");
            given(tokenProvider.generateRefreshToken(anyString())).willReturn("refreshToken123");

            // When
            AuthResponse response = authService.registerPatient(validRegisterRequest);

            // Then
            assertNotNull(response);
            verify(patientRepository, times(1)).save(any(Patient.class));
        }
    }

    @Nested
    @DisplayName("Giris (Login) Testleri")
    class GirisTestleri {

        @Test
        @DisplayName("Basarili giris - dogru email ve sifre")
        void login_BasariliGiris() {
            // Given
            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.of(testPatient));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(tokenProvider.generateAccessToken(any(UserPrincipal.class))).willReturn("accessToken123");
            given(tokenProvider.generateRefreshToken(anyString())).willReturn("refreshToken123");

            // When
            AuthResponse response = authService.login(validLoginRequest);

            // Then
            assertNotNull(response);
            assertEquals("accessToken123", response.getAccessToken());
            assertEquals("refreshToken123", response.getRefreshToken());
            assertEquals("Giris basarili", response.getMessage());
        }

        @Test
        @DisplayName("Yanlis sifre - hata firlatmali ve basarisiz giris sayisi artmali")
        void login_YanlisSifre() {
            // Given
            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.of(testPatient));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.login(validLoginRequest);
            });

            // Basarisiz giris sayisi artmali
            verify(patientRepository, times(1)).save(any(Patient.class));
        }

        @Test
        @DisplayName("Kullanici bulunamadi - hata firlatmali")
        void login_KullaniciBulunamadi() {
            // Given
            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.empty());
            given(patientRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(doctorRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(adminRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.login(validLoginRequest);
            });
        }

        @Test
        @DisplayName("Hesap kilitli - hata firlatmali")
        void login_HesapKilitli() {
            // Given
            testPatient.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));
            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.of(testPatient));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.login(validLoginRequest);
            });
        }

        @Test
        @DisplayName("Hesap askiya alinmis - hata firlatmali")
        void login_HesapAskida() {
            // Given
            testPatient.setStatus(UserStatus.SUSPENDED);
            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.of(testPatient));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.login(validLoginRequest);
            });
        }

        @Test
        @DisplayName("Hesap silinmis - hata firlatmali")
        void login_HesapSilinmis() {
            // Given
            testPatient.setStatus(UserStatus.DELETED);
            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.of(testPatient));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.login(validLoginRequest);
            });
        }

        @Test
        @DisplayName("Doktor ile basarili giris")
        void login_DoktorBasariliGiris() {
            // Given
            Doctor testDoctor = Doctor.builder()
                .firstName("Dr. Mehmet")
                .lastName("Ozturk")
                .email("dr.mehmet@test.com")
                .password("encodedPassword")
                .role(UserRole.DOCTOR)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
            testDoctor.setId("doctor123");

            given(userRepository.findByEmailOrPhone(anyString())).willReturn(Optional.empty());
            given(patientRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(doctorRepository.findByEmail(anyString())).willReturn(Optional.of(testDoctor));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(tokenProvider.generateAccessToken(any(UserPrincipal.class))).willReturn("accessToken123");
            given(tokenProvider.generateRefreshToken(anyString())).willReturn("refreshToken123");

            // When
            AuthResponse response = authService.login(validLoginRequest);

            // Then
            assertNotNull(response);
            assertEquals(UserRole.DOCTOR, response.getRole());
        }
    }

    @Nested
    @DisplayName("Token Yenileme Testleri")
    class TokenYenilemeTestleri {

        @Test
        @DisplayName("Gecerli refresh token ile yenileme")
        void refreshToken_GecerliToken() {
            // Given
            String refreshToken = "validRefreshToken123";
            given(tokenProvider.validateToken(refreshToken)).willReturn(true);
            given(tokenProvider.getUserIdFromToken(refreshToken)).willReturn("patient123");
            given(userRepository.findById("patient123")).willReturn(Optional.of(testPatient));
            given(tokenProvider.generateAccessToken(any(UserPrincipal.class))).willReturn("newAccessToken");
            given(tokenProvider.generateRefreshToken(anyString())).willReturn("newRefreshToken");

            // When
            AuthResponse response = authService.refreshToken(refreshToken);

            // Then
            assertNotNull(response);
            assertEquals("newAccessToken", response.getAccessToken());
        }

        @Test
        @DisplayName("Gecersiz refresh token - hata firlatmali")
        void refreshToken_GecersizToken() {
            // Given
            String invalidToken = "invalidToken";
            given(tokenProvider.validateToken(invalidToken)).willReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.refreshToken(invalidToken);
            });
        }

        @Test
        @DisplayName("Token gecerli ama kullanici bulunamadi")
        void refreshToken_KullaniciBulunamadi() {
            // Given
            String refreshToken = "validRefreshToken123";
            given(tokenProvider.validateToken(refreshToken)).willReturn(true);
            given(tokenProvider.getUserIdFromToken(refreshToken)).willReturn("unknownUser");
            given(userRepository.findById("unknownUser")).willReturn(Optional.empty());
            given(patientRepository.findById("unknownUser")).willReturn(Optional.empty());
            given(doctorRepository.findById("unknownUser")).willReturn(Optional.empty());
            given(adminRepository.findById("unknownUser")).willReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.refreshToken(refreshToken);
            });
        }
    }

    @Nested
    @DisplayName("Sifre Islemleri Testleri")
    class SifreIslemleriTestleri {

        @Test
        @DisplayName("Sifremi unuttum - kullanici bulunursa islem baslar")
        void forgotPassword_KullaniciBulundu() {
            // Given
            String email = "ahmet@test.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.of(testPatient));

            // When & Then - Henuz implement edilmemis, UnsupportedOperationException beklenir
            assertThrows(UnsupportedOperationException.class, () -> {
                authService.forgotPassword(email);
            });
        }

        @Test
        @DisplayName("Sifremi unuttum - kullanici bulunamazsa hata")
        void forgotPassword_KullaniciBulunamadi() {
            // Given
            String email = "bilinmeyen@test.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());
            given(patientRepository.findByEmail(email)).willReturn(Optional.empty());
            given(doctorRepository.findByEmail(email)).willReturn(Optional.empty());
            given(adminRepository.findByEmail(email)).willReturn(Optional.empty());

            // When & Then
            assertThrows(BusinessException.class, () -> {
                authService.forgotPassword(email);
            });
        }
    }
}
