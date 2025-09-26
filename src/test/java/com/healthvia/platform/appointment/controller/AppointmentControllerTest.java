package com.healthvia.platform.appointment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.entity.Appointment.AppointmentStatus;
import com.healthvia.platform.appointment.entity.Appointment.ConsultationType;
import com.healthvia.platform.appointment.service.AppointmentService;
import com.healthvia.platform.auth.security.JwtAuthenticationFilter;
import com.healthvia.platform.auth.security.JwtTokenProvider;
import com.healthvia.platform.auth.service.CustomUserDetailsService;
import com.healthvia.platform.common.exception.ResourceNotFoundException;

@WebMvcTest(
    controllers = AppointmentController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class}
    )
)
@ContextConfiguration(classes = {AppointmentController.class})
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Appointment sampleAppointment;
    private String patientId;
    private String doctorId;
    private String slotId;
    private String appointmentId;

    @BeforeEach
    void setUp() {
        patientId = "patient123";
        doctorId = "doctor456";
        slotId = "slot789";
        appointmentId = "appointment001";

        sampleAppointment = createSampleAppointment();
    }

    private Appointment createSampleAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);  // appointment001
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentDate(LocalDate.now().plusDays(5));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setDurationMinutes(30);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setConsultationType(ConsultationType.IN_PERSON);
        appointment.setChiefComplaint("Baş ağrısı");
        appointment.setConsultationFee(new java.math.BigDecimal("200.00"));
        appointment.setCurrency("TRY");
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment.setDeleted(false);
        return appointment;
    }

    @Nested
    @DisplayName("Hasta Endpoint Testleri")
    class PatientEndpointTests {

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Yeni randevu oluşturma - Başarılı")
        void createAppointment_Success() throws Exception {
            // Given
            String treatmentTypeId = "treatment123";
            String chiefComplaint = "Baş ağrısı ve halsizlik";
            
            given(appointmentService.bookAppointment(
                eq(patientId), 
                eq(doctorId), 
                eq(slotId),
                eq(treatmentTypeId),
                anyString()
            )).willReturn(sampleAppointment);

            // When & Then
            mockMvc.perform(post("/api/v1/appointments")
                    .with(csrf())
                    .param("patientId", patientId)
                    .param("doctorId", doctorId)
                    .param("slotId", slotId)
                    .param("treatmentTypeId", treatmentTypeId)
                    .param("chiefComplaint", chiefComplaint)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Randevu başarıyla oluşturuldu"))
                    .andExpect(jsonPath("$.data.id").value(appointmentId))
                    .andExpect(jsonPath("$.data.patientId").value(patientId))
                    .andExpect(jsonPath("$.data.doctorId").value(doctorId))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            verify(appointmentService, times(1)).bookAppointment(
                eq(patientId), eq(doctorId), eq(slotId), eq(treatmentTypeId), anyString());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Hasta randevularını getirme - Sayfalı")
        void getPatientAppointments_Success() throws Exception {
            // Given
            List<Appointment> appointments = Arrays.asList(sampleAppointment, createSampleAppointment());
            Page<Appointment> appointmentPage = new PageImpl<>(appointments, PageRequest.of(0, 20), 2);
            
            given(appointmentService.findByPatientId(eq(patientId), org.mockito.Mockito.any(Pageable.class)))
                .willReturn(appointmentPage);

            // When & Then
            mockMvc.perform(get("/api/v1/appointments/patient/{patientId}", patientId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Randevu detayı getirme - Başarılı")
        void getAppointment_Success() throws Exception {
            // Given
            given(appointmentService.findById(appointmentId))
                .willReturn(Optional.of(sampleAppointment));

            // When & Then
            mockMvc.perform(get("/api/v1/appointments/{id}", appointmentId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(appointmentId));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("Randevu iptal etme - Başarılı")
        void cancelAppointment_Success() throws Exception {
            // Given
            String cancelledBy = patientId;
            String reason = "Acil durum nedeniyle";
            
            Appointment cancelledAppointment = createSampleAppointment();
            cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
            cancelledAppointment.setCancelledBy(cancelledBy);
            cancelledAppointment.setCancellationReason(reason);
            
            given(appointmentService.cancelAppointment(appointmentId, cancelledBy, reason))
                .willReturn(cancelledAppointment);

            // When & Then
            mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appointmentId)
                    .with(csrf())
                    .param("cancelledBy", cancelledBy)
                    .param("reason", reason)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Randevu başarıyla iptal edildi"))
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("Doktor Endpoint Testleri")
    class DoctorEndpointTests {

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Bugünkü randevuları getirme")
        void getTodayAppointments_Success() throws Exception {
            // Given
            List<Appointment> todayAppointments = Arrays.asList(sampleAppointment);
            given(appointmentService.findTodayAppointments(doctorId))
                .willReturn(todayAppointments);

            // When & Then
            mockMvc.perform(get("/api/v1/appointments/doctor/{doctorId}/today", doctorId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Randevu onaylama - Başarılı")
        void confirmAppointment_Success() throws Exception {
            // Given
            String confirmedBy = doctorId;
            Appointment confirmedAppointment = createSampleAppointment();
            confirmedAppointment.setStatus(AppointmentStatus.CONFIRMED);
            confirmedAppointment.setConfirmedBy(confirmedBy);
            
            given(appointmentService.confirmAppointment(appointmentId, confirmedBy))
                .willReturn(confirmedAppointment);

            // When & Then
            mockMvc.perform(patch("/api/v1/appointments/{id}/confirm", appointmentId)
                    .with(csrf())
                    .param("confirmedBy", confirmedBy)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Randevu onaylandı"))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Muayene başlatma - Başarılı")
        void startConsultation_Success() throws Exception {
            // Given
            Appointment inProgressAppointment = createSampleAppointment();
            inProgressAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
            inProgressAppointment.setConsultationStartedAt(LocalDateTime.now());
            
            given(appointmentService.startConsultation(appointmentId, doctorId))
                .willReturn(inProgressAppointment);

            // When & Then
            mockMvc.perform(patch("/api/v1/appointments/{id}/start", appointmentId)
                    .with(csrf())
                    .param("doctorId", doctorId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Muayene başlatıldı"))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("Randevu tamamlama - Başarılı")
        void completeAppointment_Success() throws Exception {
            // Given
            String notes = "Hasta muayene edildi, antibiyotik reçete edildi";
            String prescriptionId = "prescription123";
            
            Appointment completedAppointment = createSampleAppointment();
            completedAppointment.setStatus(AppointmentStatus.COMPLETED);
            completedAppointment.setDoctorNotes(notes);
            completedAppointment.setPrescriptionId(prescriptionId);
            
            given(appointmentService.completeAppointment(appointmentId, doctorId, notes, prescriptionId))
                .willReturn(completedAppointment);

            // When & Then
            mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appointmentId)
                    .with(csrf())
                    .param("doctorId", doctorId)
                    .param("notes", notes)
                    .param("prescriptionId", prescriptionId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Randevu tamamlandı"))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("Admin Endpoint Testleri")
    class AdminEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Tüm randevuları getirme - Filtresiz")
        void getAllAppointments_NoFilter() throws Exception {
            // Given
            List<Appointment> appointments = Arrays.asList(
                createAppointmentWithStatus(AppointmentStatus.PENDING),
                createAppointmentWithStatus(AppointmentStatus.CONFIRMED),
                createAppointmentWithStatus(AppointmentStatus.COMPLETED)
            );
            Page<Appointment> appointmentPage = new PageImpl<>(appointments);
            
            given(appointmentService.findAll(org.mockito.Mockito.any(Pageable.class)))
                .willReturn(appointmentPage);

            // When & Then
            mockMvc.perform(get("/api/v1/appointments/admin/all")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(3)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Randevu istatistiklerini getirme - Genel")
        void getAppointmentStatistics_General() throws Exception {
            // Given
            given(appointmentService.countAppointmentsByStatus(AppointmentStatus.PENDING)).willReturn(10L);
            given(appointmentService.countAppointmentsByStatus(AppointmentStatus.CONFIRMED)).willReturn(15L);
            given(appointmentService.countAppointmentsByStatus(AppointmentStatus.COMPLETED)).willReturn(20L);
            given(appointmentService.countAppointmentsByStatus(AppointmentStatus.CANCELLED)).willReturn(5L);

            // When & Then
            mockMvc.perform(get("/api/v1/appointments/admin/statistics")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalAppointments").value(45))
                    .andExpect(jsonPath("$.data.pendingAppointments").value(10))
                    .andExpect(jsonPath("$.data.confirmedAppointments").value(15))
                    .andExpect(jsonPath("$.data.completedAppointments").value(20))
                    .andExpect(jsonPath("$.data.cancelledAppointments").value(5));
        }
    }

    @Nested
    @DisplayName("Genel Endpoint Testleri")
    class GeneralEndpointTests {

        @Test
        @WithMockUser
        @DisplayName("Yaklaşan randevuları getirme")
        void getUpcomingAppointments_Success() throws Exception {
            // Given
            String userId = "user123";
            int days = 7;
            List<Appointment> upcomingAppointments = Arrays.asList(
                createAppointmentForDate(LocalDate.now().plusDays(1)),
                createAppointmentForDate(LocalDate.now().plusDays(3))
            );
            
            given(appointmentService.findUpcomingAppointments(userId, days))
                .willReturn(upcomingAppointments);

            // When & Then
            mockMvc.perform(get("/api/v1/appointments/upcoming/{userId}", userId)
                    .param("days", String.valueOf(days))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Randevu hatırlatması gönderme")
        void sendAppointmentReminder_Success() throws Exception {
            // Given
            doNothing().when(appointmentService).sendAppointmentReminder(appointmentId);

            // When & Then
            mockMvc.perform(post("/api/v1/appointments/{id}/send-reminder", appointmentId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Hatırlatma gönderildi"));
            
            verify(appointmentService).sendAppointmentReminder(appointmentId);
        }
    }

    // Helper methods
    private Appointment createAppointmentWithStatus(AppointmentStatus status) {
        Appointment appointment = createSampleAppointment();
        appointment.setStatus(status);
        return appointment;
    }

    private Appointment createAppointmentForDate(LocalDate date) {
        Appointment appointment = createSampleAppointment();
        appointment.setAppointmentDate(date);
        return appointment;
    }
}