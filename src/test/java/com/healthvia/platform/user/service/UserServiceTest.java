package com.healthvia.platform.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.healthvia.platform.common.enums.Language;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.user.entity.User;
import com.healthvia.platform.user.repository.UserRepository;
import com.healthvia.platform.user.service.impl.UserServiceImpl;

/**
 * UserServiceImpl icin unit testleri
 * Kullanici CRUD islemleri ve hesap yonetimi test edilir
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService Testleri")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user123";

        testUser = User.builder()
            .firstName("Ahmet")
            .lastName("Yilmaz")
            .email("ahmet@test.com")
            .phone("+905551234567")
            .password("EncodedPassword123@")
            .role(UserRole.PATIENT)
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .phoneVerified(false)
            .failedLoginAttempts(0)
            .build();
        testUser.setId(userId);
        testUser.setDeleted(false);
    }

    @Nested
    @DisplayName("Kullanici Olusturma Testleri")
    class KullaniciOlusturmaTestleri {

        @Test
        @DisplayName("Basarili kullanici olusturma")
        void createUser_Basarili() {
            // Given
            User newUser = User.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .email("mehmet@test.com")
                .phone("+905559876543")
                .password("StrongPass123@")
                .role(UserRole.PATIENT)
                .build();

            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(userRepository.existsByPhoneAndDeletedFalse(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(newUser);

            // When
            User result = userService.createUser(newUser);

            // Then
            assertNotNull(result);
            verify(passwordEncoder, times(1)).encode(anyString());
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Email zaten mevcut - hata")
        void createUser_EmailMevcut() {
            // Given
            User newUser = User.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .email("mevcut@test.com")
                .phone("+905559876543")
                .password("StrongPass123@")
                .role(UserRole.PATIENT)
                .build();

            given(userRepository.existsByEmailAndDeletedFalse("mevcut@test.com")).willReturn(true);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                userService.createUser(newUser);
            });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Telefon zaten mevcut - hata")
        void createUser_TelefonMevcut() {
            // Given
            User newUser = User.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .email("mehmet@test.com")
                .phone("+905559999999")
                .password("StrongPass123@")
                .role(UserRole.PATIENT)
                .build();

            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(userRepository.existsByPhoneAndDeletedFalse("+905559999999")).willReturn(true);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                userService.createUser(newUser);
            });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Eksik email - hata")
        void createUser_EksikEmail() {
            // Given
            User newUser = User.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .phone("+905559876543")
                .password("StrongPass123@")
                .role(UserRole.PATIENT)
                .build();

            // When & Then
            assertThrows(BusinessException.class, () -> {
                userService.createUser(newUser);
            });
        }

        @Test
        @DisplayName("Zayif sifre - hata")
        void createUser_ZayifSifre() {
            // Given
            User newUser = User.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .email("mehmet@test.com")
                .phone("+905559876543")
                .password("weak")
                .role(UserRole.PATIENT)
                .build();

            given(userRepository.existsByEmailAndDeletedFalse(anyString())).willReturn(false);
            given(userRepository.existsByPhoneAndDeletedFalse(anyString())).willReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> {
                userService.createUser(newUser);
            });
        }
    }

    @Nested
    @DisplayName("Kullanici Guncelleme Testleri")
    class KullaniciGuncellemeTestleri {

        @Test
        @DisplayName("Basarili kullanici guncelleme")
        void updateUser_Basarili() {
            // Given
            User updatedData = User.builder()
                .firstName("Ahmet Yeni")
                .lastName("Yilmaz")
                .email("ahmet@test.com")
                .phone("+905551234567")
                .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.updateUser(userId, updatedData);

            // Then
            assertNotNull(result);
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Kullanici bulunamadi - hata")
        void updateUser_KullaniciBulunamadi() {
            // Given
            User updatedData = User.builder()
                .firstName("Ahmet Yeni")
                .build();

            given(userRepository.findById("bilinmeyenId")).willReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                userService.updateUser("bilinmeyenId", updatedData);
            });
        }
    }

    @Nested
    @DisplayName("Kullanici Sorgulama Testleri")
    class KullaniciSorgulamaTestleri {

        @Test
        @DisplayName("ID ile kullanici bulma - basarili")
        void findById_Basarili() {
            // Given
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findById(userId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(userId, result.get().getId());
        }

        @Test
        @DisplayName("ID ile kullanici bulma - bulunamadi")
        void findById_Bulunamadi() {
            // Given
            given(userRepository.findById("bilinmeyenId")).willReturn(Optional.empty());

            // When
            Optional<User> result = userService.findById("bilinmeyenId");

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Silinmis kullanici getirilemez")
        void findById_SilinmisKullanici() {
            // Given
            testUser.setDeleted(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findById(userId);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Email ile kullanici bulma")
        void findByEmail_Basarili() {
            // Given
            given(userRepository.findByEmailAndDeletedFalse("ahmet@test.com"))
                .willReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findByEmail("ahmet@test.com");

            // Then
            assertTrue(result.isPresent());
            assertEquals("ahmet@test.com", result.get().getEmail());
        }

        @Test
        @DisplayName("Role gore kullanici listesi")
        void findByRole() {
            // Given
            List<User> patients = Arrays.asList(testUser);
            given(userRepository.findByRoleAndDeletedFalse(UserRole.PATIENT)).willReturn(patients);

            // When
            List<User> result = userService.findByRole(UserRole.PATIENT);

            // Then
            assertEquals(1, result.size());
            assertEquals(UserRole.PATIENT, result.get(0).getRole());
        }

        @Test
        @DisplayName("Kullanici arama - sayfalama ile")
        void searchUsers_Basarili() {
            // Given
            List<User> users = Arrays.asList(testUser);
            Page<User> userPage = new PageImpl<>(users);
            Pageable pageable = PageRequest.of(0, 20);

            given(userRepository.searchUsers("Ahmet", pageable)).willReturn(userPage);

            // When
            Page<User> result = userService.searchUsers("Ahmet", pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Hesap Yonetimi Testleri")
    class HesapYonetimiTestleri {

        @Test
        @DisplayName("Kullanici durumu guncelleme")
        void updateUserStatus_Basarili() {
            // Given
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.updateUserStatus(userId, UserStatus.SUSPENDED, "admin123");

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Email dogrulama")
        void verifyEmail_Basarili() {
            // Given
            testUser.setEmailVerified(false);
            testUser.setStatus(UserStatus.PENDING_VERIFICATION);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            userService.verifyEmail(userId);

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Telefon dogrulama")
        void verifyPhone_Basarili() {
            // Given
            testUser.setPhoneVerified(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            userService.verifyPhone(userId);

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Kullanici aktifle_tirme")
        void activateUser_Basarili() {
            // Given
            testUser.setStatus(UserStatus.PENDING_VERIFICATION);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.activateUser(userId, "admin123");

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Kullanici askiya alma")
        void suspendUser_Basarili() {
            // Given
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.suspendUser(userId, "Kural ihlali", "admin123");

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Profil Yonetimi Testleri")
    class ProfilYonetimiTestleri {

        @Test
        @DisplayName("Bildirim tercihlerini guncelleme")
        void updateNotificationPreferences_Basarili() {
            // Given
            List<String> preferences = Arrays.asList("EMAIL", "SMS", "PUSH");
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.updateNotificationPreferences(userId, preferences);

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Avatar guncelleme")
        void updateAvatar_Basarili() {
            // Given
            String avatarUrl = "https://example.com/avatar.jpg";
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.updateAvatar(userId, avatarUrl);

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Dil tercihini guncelleme")
        void updateLanguagePreference_Basarili() {
            // Given
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.updateLanguagePreference(userId, Language.TURKISH);

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Silme Islemleri Testleri")
    class SilmeIslemleriTestleri {

        @Test
        @DisplayName("Soft delete - basarili")
        void deleteUser_SoftDelete() {
            // Given
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            userService.deleteUser(userId, "admin123");

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Kalici silme - basarili")
        void permanentlyDeleteUser() {
            // Given
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(any(User.class));

            // When
            userService.permanentlyDeleteUser(userId, "admin123");

            // Then
            verify(userRepository, times(1)).delete(any(User.class));
        }

        @Test
        @DisplayName("Silinmis kullaniciyi geri yukleme")
        void restoreDeletedUser() {
            // Given
            testUser.setDeleted(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            User result = userService.restoreDeletedUser(userId, "admin123");

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Silinmemis kullanici geri yuklenemez")
        void restoreDeletedUser_SilinmemisKullanici() {
            // Given
            testUser.setDeleted(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                userService.restoreDeletedUser(userId, "admin123");
            });
        }
    }

    @Nested
    @DisplayName("Dogrulama Metodlari Testleri")
    class DogrulamaMetodlariTestleri {

        @Test
        @DisplayName("Email musaitlik kontrolu - musait")
        void isEmailAvailable_Musait() {
            // Given
            given(userRepository.existsByEmailAndDeletedFalse("yeni@test.com")).willReturn(false);

            // When
            boolean result = userService.isEmailAvailable("yeni@test.com");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Email musaitlik kontrolu - musait degil")
        void isEmailAvailable_MusaitDegil() {
            // Given
            given(userRepository.existsByEmailAndDeletedFalse("mevcut@test.com")).willReturn(true);

            // When
            boolean result = userService.isEmailAvailable("mevcut@test.com");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Telefon musaitlik kontrolu")
        void isPhoneAvailable() {
            // Given
            given(userRepository.existsByPhoneAndDeletedFalse("+905559999999")).willReturn(false);

            // When
            boolean result = userService.isPhoneAvailable("+905559999999");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Kullanici aktif mi kontrolu")
        void isUserActive() {
            // Given
            testUser.setStatus(UserStatus.ACTIVE);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // When
            boolean result = userService.isUserActive(userId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Kullanici silinmis mi kontrolu")
        void isUserDeleted() {
            // Given
            testUser.setDeleted(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // When
            boolean result = userService.isUserDeleted(userId);

            // Then
            assertTrue(result);
        }
    }
}
