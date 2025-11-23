package com.healthvia.platform.doctor.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthvia.platform.auth.security.JwtAuthenticationFilter;
import com.healthvia.platform.auth.security.JwtTokenProvider;
import com.healthvia.platform.auth.service.CustomUserDetailsService;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.doctor.entity.Doctor;
import com.healthvia.platform.doctor.service.DoctorService;

/**
 * DoctorController icin unit testleri
 * Doktor CRUD islemleri ve filtreleme endpointleri test edilir
 */
@WebMvcTest(
    controllers = DoctorController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class}
    )
)
@ContextConfiguration(classes = {DoctorController.class})
@DisplayName("DoctorController Testleri")
public class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Doctor testDoctor;
    private String doctorId;

    @BeforeEach
    void setUp() {
        doctorId = "doctor123";

        testDoctor = Doctor.builder()
            .firstName("Dr. Mehmet")
            .lastName("Ozturk")
            .email("dr.mehmet@test.com")
            .phone("5559876543")
            .role(UserRole.DOCTOR)
            .status(UserStatus.ACTIVE)
            .primarySpecialty("Kardiyoloji")
            .diplomaNumber("DIP123456")
            .medicalLicenseNumber("LIC789012")
            .yearsOfExperience(10)
            .averageRating(4.5)
            .totalReviews(50)
            .consultationFee(new BigDecimal("300.00"))
            .consultationDurationMinutes(30)
            .isAcceptingNewPatients(true)
            .verificationStatus(Doctor.VerificationStatus.VERIFIED)
            .province("Istanbul")
            .district("Kadikoy")
            .build();
        testDoctor.setId(doctorId);
    }

    @Nested
    @DisplayName("Public Endpoint Testleri")
    class PublicEndpointTestleri {

        @Test
        @DisplayName("Doktor arama - filtresiz")
        void searchPublicDoctors_Filtresiz() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            Page<Doctor> doctorPage = new PageImpl<>(doctors, PageRequest.of(0, 20), 1);

            given(doctorService.findDoctorsWithFilters(
                any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(doctorPage);

            // When & Then
            mockMvc.perform(get("/api/doctors/public/search")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }

        @Test
        @DisplayName("Doktor arama - uzmanlik filtresi ile")
        void searchPublicDoctors_UzmanlikFiltresi() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            Page<Doctor> doctorPage = new PageImpl<>(doctors);

            given(doctorService.findDoctorsWithFilters(
                eq("Kardiyoloji"), any(), any(), any(), any(Pageable.class)))
                .willReturn(doctorPage);

            // When & Then
            mockMvc.perform(get("/api/doctors/public/search")
                    .param("specialty", "Kardiyoloji")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Onaylanmis doktorlari getir")
        void getVerifiedDoctors() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            given(doctorService.findVerifiedDoctors()).willReturn(doctors);

            // When & Then
            mockMvc.perform(get("/api/doctors/public/verified")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Musait doktorlari getir")
        void getAvailableDoctors() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            given(doctorService.findAvailableDoctors()).willReturn(doctors);

            // When & Then
            mockMvc.perform(get("/api/doctors/public/available")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Doktor profili getir - ID ile")
        void getPublicDoctorProfile_Basarili() throws Exception {
            // Given
            given(doctorService.findById(doctorId)).willReturn(Optional.of(testDoctor));

            // When & Then
            mockMvc.perform(get("/api/doctors/public/{id}", doctorId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.firstName").value("Dr. Mehmet"));
        }

        @Test
        @DisplayName("Doktor profili getir - bulunamadi")
        void getPublicDoctorProfile_Bulunamadi() throws Exception {
            // Given
            given(doctorService.findById("bilinmeyenId")).willReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/doctors/public/{id}", "bilinmeyenId")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Doktor Profil Yonetimi Testleri")
    class DoktorProfilYonetimiTestleri {

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Kendi profilini guncelleme")
        void updateMyProfile() throws Exception {
            // Given
            given(doctorService.updateProfile(any(), any(), any())).willReturn(testDoctor);

            // When & Then
            mockMvc.perform(patch("/api/doctors/me/profile")
                    .with(csrf())
                    .param("biography", "Yeni biyografi")
                    .param("curriculum", "Yeni ozgecmis")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Calisma saatlerini guncelleme")
        void updateMyWorkingHours() throws Exception {
            // Given
            given(doctorService.updateWorkingHours(any(), any(), any(), any())).willReturn(testDoctor);

            // When & Then
            mockMvc.perform(patch("/api/doctors/me/working-hours")
                    .with(csrf())
                    .param("workingDays", "MONDAY", "TUESDAY", "WEDNESDAY")
                    .param("startTime", "09:00")
                    .param("endTime", "17:00")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Muayene bilgilerini guncelleme")
        void updateMyConsultationInfo() throws Exception {
            // Given
            given(doctorService.updateConsultationInfo(any(), any(), any())).willReturn(testDoctor);

            // When & Then
            mockMvc.perform(patch("/api/doctors/me/consultation")
                    .with(csrf())
                    .param("fee", "350.00")
                    .param("duration", "45")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Hasta rolunde profil guncelleme - yetkisiz")
        void updateMyProfile_Yetkisiz() throws Exception {
            // When & Then
            mockMvc.perform(patch("/api/doctors/me/profile")
                    .with(csrf())
                    .param("biography", "Yeni biyografi")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Admin Endpoint Testleri")
    class AdminEndpointTestleri {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Tum doktorlari getir")
        void getAllDoctors() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            Page<Doctor> doctorPage = new PageImpl<>(doctors);

            given(doctorService.findAll(any(Pageable.class))).willReturn(doctorPage);

            // When & Then
            mockMvc.perform(get("/api/doctors")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Doktor arama - admin")
        void searchDoctors_Admin() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            Page<Doctor> doctorPage = new PageImpl<>(doctors);

            given(doctorService.searchDoctors(eq("Mehmet"), any(Pageable.class)))
                .willReturn(doctorPage);

            // When & Then
            mockMvc.perform(get("/api/doctors/search")
                    .param("searchTerm", "Mehmet")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Doktor dogrulama durumu guncelleme")
        void updateVerificationStatus() throws Exception {
            // Given
            testDoctor.setVerificationStatus(Doctor.VerificationStatus.VERIFIED);
            given(doctorService.updateVerificationStatus(eq(doctorId), any()))
                .willReturn(testDoctor);

            // When & Then
            mockMvc.perform(patch("/api/doctors/{id}/verification", doctorId)
                    .with(csrf())
                    .param("status", "VERIFIED")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Verification status updated"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Doktor silme")
        void deleteDoctor() throws Exception {
            // Given
            doNothing().when(doctorService).deleteDoctor(eq(doctorId), any());

            // When & Then
            mockMvc.perform(delete("/api/doctors/{id}", doctorId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(doctorService, times(1)).deleteDoctor(eq(doctorId), any());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Admin endpoint'e hasta erisimi - yetkisiz")
        void getAllDoctors_Yetkisiz() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/doctors")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Filtreleme Endpoint Testleri")
    class FiltrelemeEndpointTestleri {

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Uzmanliga gore doktor listesi")
        void getDoctorsBySpecialty() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            given(doctorService.findBySpecialty("Kardiyoloji")).willReturn(doctors);

            // When & Then
            mockMvc.perform(get("/api/doctors/by-specialty/{specialty}", "Kardiyoloji")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Konuma gore doktor listesi")
        void getDoctorsByLocation() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            given(doctorService.findByLocation("Istanbul", "Kadikoy")).willReturn(doctors);

            // When & Then
            mockMvc.perform(get("/api/doctors/by-location")
                    .param("province", "Istanbul")
                    .param("district", "Kadikoy")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("En yuksek puanli doktorlar")
        void getTopRatedDoctors() throws Exception {
            // Given
            List<Doctor> doctors = Arrays.asList(testDoctor);
            given(doctorService.findTopRatedDoctors(10)).willReturn(doctors);

            // When & Then
            mockMvc.perform(get("/api/doctors/top-rated")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Dogrulama Endpoint Testleri")
    class DogrulamaEndpointTestleri {

        @Test
        @DisplayName("Diploma numarasi musaitlik kontrolu - musait")
        void checkDiplomaAvailability_Musait() throws Exception {
            // Given
            given(doctorService.isDiplomaNumberAvailable("DIP999999")).willReturn(true);

            // When & Then
            mockMvc.perform(get("/api/doctors/check-diploma")
                    .param("diplomaNumber", "DIP999999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("Diploma numarasi musaitlik kontrolu - musait degil")
        void checkDiplomaAvailability_MusaitDegil() throws Exception {
            // Given
            given(doctorService.isDiplomaNumberAvailable("DIP123456")).willReturn(false);

            // When & Then
            mockMvc.perform(get("/api/doctors/check-diploma")
                    .param("diplomaNumber", "DIP123456")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }

        @Test
        @DisplayName("Lisans numarasi musaitlik kontrolu")
        void checkLicenseAvailability() throws Exception {
            // Given
            given(doctorService.isMedicalLicenseNumberAvailable("LIC999999")).willReturn(true);

            // When & Then
            mockMvc.perform(get("/api/doctors/check-license")
                    .param("licenseNumber", "LIC999999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }
    }

    @Nested
    @DisplayName("Istatistik Endpoint Testleri")
    class IstatistikEndpointTestleri {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Onaylanmis doktor sayisi")
        void getVerifiedDoctorsCount() throws Exception {
            // Given
            given(doctorService.countVerifiedDoctors()).willReturn(100L);

            // When & Then
            mockMvc.perform(get("/api/doctors/statistics/count-verified")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(100));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Uzmanliga gore doktor sayisi")
        void getDoctorsCountBySpecialty() throws Exception {
            // Given
            given(doctorService.countDoctorsBySpecialty("Kardiyoloji")).willReturn(25L);

            // When & Then
            mockMvc.perform(get("/api/doctors/statistics/count-by-specialty")
                    .param("specialty", "Kardiyoloji")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(25));
        }
    }
}
