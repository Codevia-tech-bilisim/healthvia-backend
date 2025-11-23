package com.healthvia.platform.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;

/**
 * JwtTokenProvider icin unit testleri
 * JWT token olusturma, dogrulama ve bilgi cikarma islemlerini test eder
 */
@DisplayName("JwtTokenProvider Testleri")
public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        // JWT ozelliklerini ayarla
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B59703373367639792442264529482B4D6251655468576D5A7134743777217A25432A");
        jwtProperties.setAccessTokenExpiration(900000); // 15 dakika
        jwtProperties.setRefreshTokenExpiration(604800000); // 7 gun

        // Token provider olustur ve baslat
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();

        // Test kullanicisi olustur
        testUserPrincipal = UserPrincipal.builder()
            .id("user123")
            .email("test@example.com")
            .password("encodedPassword")
            .firstName("Ahmet")
            .lastName("Yilmaz")
            .role(UserRole.PATIENT)
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .build();
    }

    @Nested
    @DisplayName("Access Token Olusturma Testleri")
    class AccessTokenOlusturmaTestleri {

        @Test
        @DisplayName("Gecerli kullanici ile access token olusturma")
        void generateAccessToken_GecerliKullanici() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);

            // Then
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT formati: header.payload.signature
        }

        @Test
        @DisplayName("Olusturulan token gecerli olmali")
        void generateAccessToken_TokenGecerliOlmali() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);

            // Then
            assertTrue(jwtTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("Token icinden kullanici ID cikartma")
        void generateAccessToken_KullaniciIdCikartma() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);
            String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // Then
            assertEquals("user123", extractedUserId);
        }

        @Test
        @DisplayName("Token icinden rol bilgisi cikartma")
        void generateAccessToken_RolCikartma() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);
            UserRole extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // Then
            assertEquals(UserRole.PATIENT, extractedRole);
        }

        @Test
        @DisplayName("Token icinden email bilgisi cikartma")
        void generateAccessToken_EmailCikartma() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);
            String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

            // Then
            assertEquals("test@example.com", extractedEmail);
        }

        @Test
        @DisplayName("Token icinden tam ad bilgisi cikartma")
        void generateAccessToken_TamAdCikartma() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);
            String extractedFullName = jwtTokenProvider.getFullNameFromToken(token);

            // Then
            assertEquals("Ahmet Yilmaz", extractedFullName);
        }

        @Test
        @DisplayName("Farkli roller icin token olusturma")
        void generateAccessToken_FarkliRoller() {
            // Given - Doktor kullanicisi
            UserPrincipal doctorPrincipal = UserPrincipal.builder()
                .id("doctor456")
                .email("doctor@example.com")
                .password("encodedPassword")
                .firstName("Dr. Mehmet")
                .lastName("Ozturk")
                .role(UserRole.DOCTOR)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

            // When
            String token = jwtTokenProvider.generateAccessToken(doctorPrincipal);
            UserRole extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // Then
            assertEquals(UserRole.DOCTOR, extractedRole);
        }

        @Test
        @DisplayName("Admin icin token olusturma")
        void generateAccessToken_AdminRolu() {
            // Given - Admin kullanicisi
            UserPrincipal adminPrincipal = UserPrincipal.builder()
                .id("admin789")
                .email("admin@example.com")
                .password("encodedPassword")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

            // When
            String token = jwtTokenProvider.generateAccessToken(adminPrincipal);
            UserRole extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // Then
            assertEquals(UserRole.ADMIN, extractedRole);
        }
    }

    @Nested
    @DisplayName("Refresh Token Olusturma Testleri")
    class RefreshTokenOlusturmaTestleri {

        @Test
        @DisplayName("Gecerli kullanici ID ile refresh token olusturma")
        void generateRefreshToken_GecerliKullaniciId() {
            // When
            String refreshToken = jwtTokenProvider.generateRefreshToken("user123");

            // Then
            assertNotNull(refreshToken);
            assertFalse(refreshToken.isEmpty());
            assertTrue(refreshToken.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Refresh token gecerli olmali")
        void generateRefreshToken_TokenGecerliOlmali() {
            // When
            String refreshToken = jwtTokenProvider.generateRefreshToken("user123");

            // Then
            assertTrue(jwtTokenProvider.validateToken(refreshToken));
        }

        @Test
        @DisplayName("Refresh token icinden kullanici ID cikartma")
        void generateRefreshToken_KullaniciIdCikartma() {
            // When
            String refreshToken = jwtTokenProvider.generateRefreshToken("user123");
            String extractedUserId = jwtTokenProvider.getUserIdFromToken(refreshToken);

            // Then
            assertEquals("user123", extractedUserId);
        }
    }

    @Nested
    @DisplayName("Token Dogrulama Testleri")
    class TokenDogrulamaTestleri {

        @Test
        @DisplayName("Gecerli token - true donmeli")
        void validateToken_GecerliToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Bozuk token - false donmeli")
        void validateToken_BozukToken() {
            // Given
            String malformedToken = "bu.gecersiz.token";

            // When
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Bos token - false donmeli")
        void validateToken_BosToken() {
            // Given
            String emptyToken = "";

            // When
            boolean isValid = jwtTokenProvider.validateToken(emptyToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Null token - false donmeli")
        void validateToken_NullToken() {
            // When
            boolean isValid = jwtTokenProvider.validateToken(null);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Duzenlenmis token - false donmeli")
        void validateToken_DuzenlenmisPaylod() {
            // Given - Gecerli token olustur ve payload'u degistir
            String validToken = jwtTokenProvider.generateAccessToken(testUserPrincipal);
            String[] parts = validToken.split("\\.");
            String tamperedToken = parts[0] + ".YWJjMTIz." + parts[2]; // Payload degistirildi

            // When
            boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Suresi dolmus token testi icin kisa sureli token")
        void validateToken_SuresiDolmusToken() {
            // Given - Cok kisa sureli token icin yeni JwtProperties
            JwtProperties shortLivedProperties = new JwtProperties();
            shortLivedProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B59703373367639792442264529482B4D6251655468576D5A7134743777217A25432A");
            shortLivedProperties.setAccessTokenExpiration(1); // 1 milisaniye

            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLivedProperties);
            shortLivedProvider.init();

            String token = shortLivedProvider.generateAccessToken(testUserPrincipal);

            // Kisa bir sure bekle
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            boolean isValid = shortLivedProvider.validateToken(token);

            // Then
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("Token Bilgi Cikartma Testleri")
    class TokenBilgiCikartmaTestleri {

        @Test
        @DisplayName("Gecerli tokenden tum bilgileri cikarma")
        void extractAllInfo_GecerliToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(testUserPrincipal);

            // When
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            String fullName = jwtTokenProvider.getFullNameFromToken(token);
            UserRole role = jwtTokenProvider.getRoleFromToken(token);

            // Then
            assertAll("Tum bilgiler dogru olmali",
                () -> assertEquals("user123", userId),
                () -> assertEquals("test@example.com", email),
                () -> assertEquals("Ahmet Yilmaz", fullName),
                () -> assertEquals(UserRole.PATIENT, role)
            );
        }
    }

    @Nested
    @DisplayName("Farkli Kullanici Tipleri Testleri")
    class FarkliKullaniciTipleriTestleri {

        @Test
        @DisplayName("Hasta tokeni")
        void hastaTokeni() {
            // Given
            UserPrincipal hastaPrincipal = UserPrincipal.builder()
                .id("patient001")
                .email("hasta@test.com")
                .firstName("Ali")
                .lastName("Veli")
                .role(UserRole.PATIENT)
                .status(UserStatus.ACTIVE)
                .build();

            // When
            String token = jwtTokenProvider.generateAccessToken(hastaPrincipal);

            // Then
            assertEquals(UserRole.PATIENT, jwtTokenProvider.getRoleFromToken(token));
            assertEquals("patient001", jwtTokenProvider.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("Doktor tokeni")
        void doktorTokeni() {
            // Given
            UserPrincipal doktorPrincipal = UserPrincipal.builder()
                .id("doctor001")
                .email("doktor@test.com")
                .firstName("Dr. Ayse")
                .lastName("Kaya")
                .role(UserRole.DOCTOR)
                .status(UserStatus.ACTIVE)
                .build();

            // When
            String token = jwtTokenProvider.generateAccessToken(doktorPrincipal);

            // Then
            assertEquals(UserRole.DOCTOR, jwtTokenProvider.getRoleFromToken(token));
            assertEquals("doctor001", jwtTokenProvider.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("Admin tokeni")
        void adminTokeni() {
            // Given
            UserPrincipal adminPrincipal = UserPrincipal.builder()
                .id("admin001")
                .email("admin@test.com")
                .firstName("Sistem")
                .lastName("Yoneticisi")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

            // When
            String token = jwtTokenProvider.generateAccessToken(adminPrincipal);

            // Then
            assertEquals(UserRole.ADMIN, jwtTokenProvider.getRoleFromToken(token));
            assertEquals("admin001", jwtTokenProvider.getUserIdFromToken(token));
        }
    }
}
