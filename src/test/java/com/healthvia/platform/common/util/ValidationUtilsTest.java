package com.healthvia.platform.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ValidationUtils icin unit testleri
 * Girdi dogrulama ve temizleme islemleri test edilir
 */
@DisplayName("ValidationUtils Testleri")
public class ValidationUtilsTest {

    @Nested
    @DisplayName("URL Parametre Decode Testleri")
    class UrlParametreDecodeTestleri {

        @Test
        @DisplayName("Turkce karakterli parametre decode")
        void decodeUrlParameter_TurkceKarakterler() {
            // Given - "Baş ağrısı" URL encoded
            String encoded = "Ba%C5%9F%20a%C4%9Fr%C4%B1s%C4%B1";

            // When
            String result = ValidationUtils.decodeUrlParameter(encoded);

            // Then - Turkce karakterler: ş=\u015f, ğ=\u011f, ı=\u0131
            assertEquals("Ba\u015f a\u011fr\u0131s\u0131", result);
        }

        @Test
        @DisplayName("Normal metin decode")
        void decodeUrlParameter_NormalMetin() {
            // Given
            String input = "normal metin";

            // When
            String result = ValidationUtils.decodeUrlParameter(input);

            // Then
            assertEquals("normal metin", result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null veya bos parametre")
        void decodeUrlParameter_NullVeyaBos(String input) {
            // When
            String result = ValidationUtils.decodeUrlParameter(input);

            // Then
            assertEquals(input, result);
        }

        @Test
        @DisplayName("Ozel karakterler decode")
        void decodeUrlParameter_OzelKarakterler() {
            // Given
            String encoded = "test%2Fvalue%3Fquery%3D1"; // "test/value?query=1"

            // When
            String result = ValidationUtils.decodeUrlParameter(encoded);

            // Then
            assertEquals("test/value?query=1", result);
        }
    }

    @Nested
    @DisplayName("Turkce Isim Dogrulama Testleri")
    class TurkceIsimDogrulamaTestleri {

        @ParameterizedTest
        @ValueSource(strings = {"Ahmet", "Mehmet Ali", "Ayse", "Fatma-Nur", "O'Brien"})
        @DisplayName("Gecerli Turkce isimler")
        void isValidTurkishName_GecerliIsimler(String name) {
            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Turkce ozel karakterli isimler - Omer")
        void isValidTurkishName_TurkceKarakterliIsimler_Omer() {
            // Given - Ö=\u00d6
            String name = "\u00d6mer";

            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Turkce ozel karakterli isimler - Cagla")
        void isValidTurkishName_TurkceKarakterliIsimler_Cagla() {
            // Given - Ç=\u00c7, ğ=\u011f
            String name = "\u00c7a\u011fla";

            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Turkce ozel karakterli isimler - Gulsen")
        void isValidTurkishName_TurkceKarakterliIsimler_Gulsen() {
            // Given - ü=\u00fc, ş=\u015f
            String name = "G\u00fcl\u015fen";

            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertTrue(result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null veya bos isim - gecersiz")
        void isValidTurkishName_NullVeyaBos(String name) {
            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Tek karakterli isim - gecersiz (minimum 2)")
        void isValidTurkishName_TekKarakter() {
            // Given
            String name = "A";

            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Cok uzun isim - gecersiz (maksimum 50)")
        void isValidTurkishName_CokUzun() {
            // Given - 51 karakterlik isim
            String name = "A".repeat(51);

            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ahmet123", "Test!", "Ali@Veli", "Mehmet#"})
        @DisplayName("Sayi veya ozel karakter iceren isim - gecersiz")
        void isValidTurkishName_SayiVeyaOzelKarakter(String name) {
            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Sadece bosluk - gecersiz")
        void isValidTurkishName_SadeceBosluk() {
            // Given
            String name = "   ";

            // When
            boolean result = ValidationUtils.isValidTurkishName(name);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Turkce Telefon Dogrulama Testleri")
    class TurkTelefonDogrulamaTestleri {

        @Test
        @DisplayName("Gecerli Turk telefon numarasi")
        void isValidTurkishPhone_GecerliNumara() {
            // Given
            String phone = "+905551234567";

            // When
            boolean result = ValidationUtils.isValidTurkishPhone(phone);

            // Then
            assertTrue(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "+905001234567",
            "+905301234567",
            "+905401234567",
            "+905501234567"
        })
        @DisplayName("Farkli operator prefiksleri")
        void isValidTurkishPhone_FarkliOperatorler(String phone) {
            // When
            boolean result = ValidationUtils.isValidTurkishPhone(phone);

            // Then
            assertTrue(result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null veya bos telefon - gecersiz")
        void isValidTurkishPhone_NullVeyaBos(String phone) {
            // When
            boolean result = ValidationUtils.isValidTurkishPhone(phone);

            // Then
            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "5551234567",       // +90 eksik
            "05551234567",      // +90 yerine 0
            "+905551234",       // Kisa numara
            "+9055512345678",   // Uzun numara
            "+1234567890123",   // Yanlis ulke kodu
            "+90 555 123 4567"  // Bosluklu
        })
        @DisplayName("Gecersiz telefon formatlari")
        void isValidTurkishPhone_GecersizFormatlar(String phone) {
            // When
            boolean result = ValidationUtils.isValidTurkishPhone(phone);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Sikayet Metni Temizleme Testleri")
    class SikayetMetniTemizlemeTestleri {

        @Test
        @DisplayName("Normal metin - degismeden doner")
        void sanitizeChiefComplaint_NormalMetin() {
            // Given
            String complaint = "Bas agrisi ve halsizlik";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals("Bas agrisi ve halsizlik", result);
        }

        @Test
        @DisplayName("Turkce karakterli metin")
        void sanitizeChiefComplaint_TurkceKarakterler() {
            // Given - Ş=\u015e, ş=\u015f, ğ=\u011f, ı=\u0131, ö=\u00f6, ü=\u00fc
            String complaint = "\u015eiddetli ba\u015f a\u011fr\u0131s\u0131, \u00f6ks\u00fcr\u00fck ve ate\u015f";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals(complaint, result);
        }

        @Test
        @DisplayName("HTML taglerini temizle - XSS onleme")
        void sanitizeChiefComplaint_HtmlTagleriTemizle() {
            // Given
            String complaint = "Bas agrisi <script>alert('xss')</script> var";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals("Bas agrisi alert('xss') var", result);
            assertFalse(result.contains("<script>"));
            assertFalse(result.contains("</script>"));
        }

        @Test
        @DisplayName("Birden fazla HTML tag temizleme")
        void sanitizeChiefComplaint_CokluHtmlTag() {
            // Given
            String complaint = "<b>Bas agrisi</b> ve <i>bulanti</i>";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals("Bas agrisi ve bulanti", result);
        }

        @Test
        @DisplayName("500 karakterden uzun metin kisaltilir")
        void sanitizeChiefComplaint_UzunMetin() {
            // Given - 600 karakterlik metin
            String complaint = "A".repeat(600);

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals(500, result.length());
        }

        @Test
        @DisplayName("Bosluklar trim edilir")
        void sanitizeChiefComplaint_BosluklariTrimle() {
            // Given
            String complaint = "   Bas agrisi   ";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals("Bas agrisi", result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null veya bos metin")
        void sanitizeChiefComplaint_NullVeyaBos(String complaint) {
            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertEquals(complaint, result);
        }

        @Test
        @DisplayName("URL encoded Turkce metin decode edilir")
        void sanitizeChiefComplaint_UrlEncodedMetin() {
            // Given - "Baş ağrısı" URL encoded
            String complaint = "Ba%C5%9F%20a%C4%9Fr%C4%B1s%C4%B1";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then - ş=\u015f, ğ=\u011f, ı=\u0131
            assertEquals("Ba\u015f a\u011fr\u0131s\u0131", result);
        }

        @Test
        @DisplayName("Karisik XSS ve URL encoded metin")
        void sanitizeChiefComplaint_KarisikXssVeEncoded() {
            // Given
            String complaint = "<script>alert('test')</script>Ba%C5%9F%20a%C4%9Fr%C4%B1s%C4%B1";

            // When
            String result = ValidationUtils.sanitizeChiefComplaint(complaint);

            // Then
            assertFalse(result.contains("<script>"));
            assertTrue(result.contains("Ba\u015f a\u011fr\u0131s\u0131"));
        }
    }

    @Nested
    @DisplayName("Entegrasyon Testleri")
    class EntegrasyonTestleri {

        @Test
        @DisplayName("Tam is akisi - URL decode, validation ve sanitize")
        void tamIsAkisi() {
            // Given - URL encoded Turkce isim "Ömer" - Ö=%C3%96
            String encodedName = "%C3%96mer";
            String decodedName = ValidationUtils.decodeUrlParameter(encodedName);

            // Then - Decode edilmis isim gecerli olmali - Ö=\u00d6
            assertEquals("\u00d6mer", decodedName);
            assertTrue(ValidationUtils.isValidTurkishName(decodedName));
        }

        @Test
        @DisplayName("Gercekci senaryo - hasta sikayeti")
        void gercekciSenaryo_HastaSikayeti() {
            // Given - Gercekci bir hasta sikayeti (Turkce karakterler Unicode escape ile)
            // Şiddetli baş ağrısı, 3 gündür devam ediyor. Ateşim var (38.5°C). Öksürük ve burun akıntısı mevcut.
            String sikayet = "\u015eiddetli ba\u015f a\u011fr\u0131s\u0131, 3 g\u00fcnd\u00fcr devam ediyor. " +
                           "Ate\u015fim var (38.5\u00b0C). \u00d6ks\u00fcr\u00fck ve burun ak\u0131nt\u0131s\u0131 mevcut.";

            // When
            String sanitized = ValidationUtils.sanitizeChiefComplaint(sikayet);

            // Then
            assertEquals(sikayet, sanitized);
            assertTrue(sanitized.length() <= 500);
        }
    }
}
