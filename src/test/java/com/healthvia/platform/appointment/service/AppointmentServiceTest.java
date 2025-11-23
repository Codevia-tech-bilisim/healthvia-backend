package com.healthvia.platform.appointment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.entity.Appointment.AppointmentStatus;
import com.healthvia.platform.appointment.entity.Appointment.ConsultationType;
import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.entity.TimeSlot.SlotStatus;
import com.healthvia.platform.appointment.repository.AppointmentRepository;
import com.healthvia.platform.appointment.service.impl.AppointmentServiceImpl;
import com.healthvia.platform.common.exception.AppointmentExceptions;
import com.healthvia.platform.common.exception.ResourceNotFoundException;

/**
 * AppointmentServiceImpl icin unit testleri
 * Randevu islemleri ve is kurallari test edilir
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppointmentService Testleri")
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeSlotService timeSlotService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Appointment testAppointment;
    private TimeSlot testSlot;
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

        // Test randevusu olustur
        testAppointment = new Appointment();
        testAppointment.setId(appointmentId);
        testAppointment.setPatientId(patientId);
        testAppointment.setDoctorId(doctorId);
        testAppointment.setAppointmentDate(LocalDate.now().plusDays(5));
        testAppointment.setStartTime(LocalTime.of(10, 0));
        testAppointment.setEndTime(LocalTime.of(10, 30));
        testAppointment.setDurationMinutes(30);
        testAppointment.setStatus(AppointmentStatus.PENDING);
        testAppointment.setConsultationType(ConsultationType.IN_PERSON);
        testAppointment.setChiefComplaint("Bas agrisi");
        testAppointment.setConsultationFee(new BigDecimal("200.00"));
        testAppointment.setCurrency("TRY");
        testAppointment.setCreatedAt(LocalDateTime.now());
        testAppointment.setUpdatedAt(LocalDateTime.now());
        testAppointment.setDeleted(false);

        // Test slot olustur
        testSlot = new TimeSlot();
        testSlot.setId(slotId);
        testSlot.setDoctorId(doctorId);
        testSlot.setDate(LocalDate.now().plusDays(5));
        testSlot.setStartTime(LocalTime.of(10, 0));
        testSlot.setEndTime(LocalTime.of(10, 30));
        testSlot.setDurationMinutes(30);
        testSlot.setStatus(SlotStatus.AVAILABLE);
        testSlot.setAvailable(true);
    }

    @Nested
    @DisplayName("Randevu Olusturma Testleri")
    class RandevuOlusturmaTestleri {

        @Test
        @DisplayName("Basarili randevu olusturma")
        void createAppointment_Basarili() {
            // Given
            given(timeSlotService.findById(slotId)).willReturn(Optional.of(testSlot));
            given(timeSlotService.updateSlot(eq(slotId), any(TimeSlot.class))).willReturn(testSlot);
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            Appointment result = appointmentService.createAppointment(
                patientId, doctorId, slotId, "Bas agrisi");

            // Then
            assertNotNull(result);
            assertEquals(patientId, result.getPatientId());
            assertEquals(doctorId, result.getDoctorId());
            assertEquals(AppointmentStatus.PENDING, result.getStatus());

            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Slot bulunamadi - hata firlatmali")
        void createAppointment_SlotBulunamadi() {
            // Given
            given(timeSlotService.findById(slotId)).willReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                appointmentService.createAppointment(patientId, doctorId, slotId, "Bas agrisi");
            });

            verify(appointmentRepository, never()).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Gecmis tarihli slot - hata firlatmali")
        void createAppointment_GecmisTarih() {
            // Given
            testSlot.setDate(LocalDate.now().minusDays(1));
            given(timeSlotService.findById(slotId)).willReturn(Optional.of(testSlot));

            // When & Then
            assertThrows(AppointmentExceptions.PastDateAppointmentException.class, () -> {
                appointmentService.createAppointment(patientId, doctorId, slotId, "Bas agrisi");
            });
        }

        @Test
        @DisplayName("Slot zaten rezerve edilmis - hata firlatmali")
        void createAppointment_SlotZatenRezerve() {
            // Given
            testSlot.setStatus(SlotStatus.BOOKED);
            testSlot.setAvailable(false);
            given(timeSlotService.findById(slotId)).willReturn(Optional.of(testSlot));

            // When & Then
            assertThrows(AppointmentExceptions.SlotAlreadyBookedException.class, () -> {
                appointmentService.createAppointment(patientId, doctorId, slotId, "Bas agrisi");
            });
        }

        @Test
        @DisplayName("Slot musait degil - hata firlatmali")
        void createAppointment_SlotMusaitDegil() {
            // Given
            testSlot.setStatus(SlotStatus.BLOCKED);
            testSlot.setAvailable(false);
            given(timeSlotService.findById(slotId)).willReturn(Optional.of(testSlot));

            // When & Then
            assertThrows(AppointmentExceptions.SlotNotAvailableException.class, () -> {
                appointmentService.createAppointment(patientId, doctorId, slotId, "Bas agrisi");
            });
        }
    }

    @Nested
    @DisplayName("Randevu Rezervasyonu (bookAppointment) Testleri")
    class RandevuRezervasyonuTestleri {

        @Test
        @DisplayName("Basarili randevu rezervasyonu")
        void bookAppointment_Basarili() {
            // Given
            String treatmentTypeId = "treatment123";
            String chiefComplaint = "Bas agrisi ve halsizlik";

            given(timeSlotService.findById(slotId)).willReturn(Optional.of(testSlot));
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);
            doNothing().when(timeSlotService).bookSlot(anyString(), anyString());

            // When
            Appointment result = appointmentService.bookAppointment(
                patientId, doctorId, slotId, treatmentTypeId, chiefComplaint);

            // Then
            assertNotNull(result);
            verify(timeSlotService, times(1)).bookSlot(eq(slotId), anyString());
        }

        @Test
        @DisplayName("Slot baska doktora ait - hata firlatmali")
        void bookAppointment_SlotBaskaDokora() {
            // Given
            testSlot.setDoctorId("baskadoktor999");
            given(timeSlotService.findById(slotId)).willReturn(Optional.of(testSlot));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                appointmentService.bookAppointment(
                    patientId, doctorId, slotId, "treatment123", "Bas agrisi");
            });
        }
    }

    @Nested
    @DisplayName("Randevu Onaylama Testleri")
    class RandevuOnaylamaTestleri {

        @Test
        @DisplayName("Basarili randevu onaylama")
        void confirmAppointment_Basarili() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            Appointment confirmedAppointment = new Appointment();
            confirmedAppointment.setId(appointmentId);
            confirmedAppointment.setStatus(AppointmentStatus.CONFIRMED);
            given(appointmentRepository.save(any(Appointment.class))).willReturn(confirmedAppointment);

            // When
            Appointment result = appointmentService.confirmAppointment(appointmentId, doctorId);

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Zaten onaylanmis randevu - hata firlatmali")
        void confirmAppointment_ZatenOnaylanmis() {
            // Given
            testAppointment.setStatus(AppointmentStatus.CONFIRMED);
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                appointmentService.confirmAppointment(appointmentId, doctorId);
            });
        }

        @Test
        @DisplayName("Randevu bulunamadi - hata firlatmali")
        void confirmAppointment_RandevuBulunamadi() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                appointmentService.confirmAppointment(appointmentId, doctorId);
            });
        }
    }

    @Nested
    @DisplayName("Randevu Iptal Testleri")
    class RandevuIptalTestleri {

        @Test
        @DisplayName("Basarili randevu iptali (24 saat onceden)")
        void cancelAppointment_Basarili() {
            // Given - 5 gun sonraki randevu
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            Appointment cancelledAppointment = new Appointment();
            cancelledAppointment.setId(appointmentId);
            cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
            given(appointmentRepository.save(any(Appointment.class))).willReturn(cancelledAppointment);

            // When
            Appointment result = appointmentService.cancelAppointment(
                appointmentId, patientId, "Acil durum");

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("24 saatten az kalmis - iptal edilemez")
        void cancelAppointment_SureDolmus() {
            // Given - Yarin sabah 10:00 randevusu (simdi aksam 22:00 diyelim)
            testAppointment.setAppointmentDate(LocalDate.now());
            testAppointment.setStartTime(LocalTime.now().plusHours(12)); // 12 saat sonra

            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When & Then
            assertThrows(AppointmentExceptions.CancellationDeadlineException.class, () -> {
                appointmentService.cancelAppointment(appointmentId, patientId, "Vazgectim");
            });
        }

        @Test
        @DisplayName("Tamamlanmis randevu iptal edilemez")
        void cancelAppointment_TamamlanmisRandevu() {
            // Given
            testAppointment.setStatus(AppointmentStatus.COMPLETED);
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When & Then - canBeCancelled() false donecek
            assertThrows(RuntimeException.class, () -> {
                appointmentService.cancelAppointment(appointmentId, patientId, "Vazgectim");
            });
        }
    }

    @Nested
    @DisplayName("Muayene Baslat/Tamamla Testleri")
    class MuayeneIslemleriTestleri {

        @Test
        @DisplayName("Muayene baslatma - basarili")
        void startConsultation_Basarili() {
            // Given
            testAppointment.setStatus(AppointmentStatus.CONFIRMED);
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            Appointment result = appointmentService.startConsultation(appointmentId, doctorId);

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Baska doktor muayene baslatamaz")
        void startConsultation_BaskaDoktor() {
            // Given
            testAppointment.setStatus(AppointmentStatus.CONFIRMED);
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                appointmentService.startConsultation(appointmentId, "baskaDoktor999");
            });
        }

        @Test
        @DisplayName("Onaylanmamis randevu icin muayene baslatma")
        void startConsultation_OnaylanmamisRandevu() {
            // Given - Status PENDING
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                appointmentService.startConsultation(appointmentId, doctorId);
            });
        }

        @Test
        @DisplayName("Randevu tamamlama - basarili")
        void completeAppointment_Basarili() {
            // Given
            testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            Appointment result = appointmentService.completeAppointment(
                appointmentId, doctorId, "Hasta muayene edildi", "prescription123");

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Devam etmiyor durumundaki randevu tamamlanamaz")
        void completeAppointment_DevamEtmiyorDurumda() {
            // Given
            testAppointment.setStatus(AppointmentStatus.CONFIRMED);
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                appointmentService.completeAppointment(
                    appointmentId, doctorId, "Notlar", "prescription123");
            });
        }
    }

    @Nested
    @DisplayName("Randevu Sorgulama Testleri")
    class RandevuSorgulamaTestleri {

        @Test
        @DisplayName("ID ile randevu bulma")
        void findById_Basarili() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            // When
            Optional<Appointment> result = appointmentService.findById(appointmentId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(appointmentId, result.get().getId());
        }

        @Test
        @DisplayName("Hasta randevularini sayfalama ile getirme")
        void findByPatientId_Basarili() {
            // Given
            List<Appointment> appointments = Arrays.asList(testAppointment);
            Page<Appointment> page = new PageImpl<>(appointments);
            Pageable pageable = PageRequest.of(0, 20);

            given(appointmentRepository.findByPatientIdAndDeletedFalse(patientId, pageable))
                .willReturn(page);

            // When
            Page<Appointment> result = appointmentService.findByPatientId(patientId, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Doktorun bugunku randevulari")
        void findTodayAppointments_Basarili() {
            // Given
            List<Appointment> todayAppointments = Arrays.asList(testAppointment);
            given(appointmentRepository.findByDoctorIdAndAppointmentDateAndDeletedFalse(
                eq(doctorId), any(LocalDate.class)))
                .willReturn(todayAppointments);

            // When
            List<Appointment> result = appointmentService.findTodayAppointments(doctorId);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Yaklasan randevular")
        void findUpcomingAppointments_Basarili() {
            // Given
            List<Appointment> upcomingAppointments = Arrays.asList(testAppointment);
            given(appointmentRepository.findUpcomingAppointments(
                eq(patientId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(upcomingAppointments);

            // When
            List<Appointment> result = appointmentService.findUpcomingAppointments(patientId, 7);

            // Then
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Randevu Istatistik Testleri")
    class RandevuIstatistikTestleri {

        @Test
        @DisplayName("Duruma gore randevu sayisi")
        void countAppointmentsByStatus() {
            // Given
            given(appointmentRepository.countByStatusAndDeletedFalse(AppointmentStatus.PENDING))
                .willReturn(10L);

            // When
            long count = appointmentService.countAppointmentsByStatus(AppointmentStatus.PENDING);

            // Then
            assertEquals(10L, count);
        }

        @Test
        @DisplayName("Doktora gore randevu sayisi")
        void countAppointmentsByDoctor() {
            // Given
            given(appointmentRepository.countByDoctorIdAndDeletedFalse(doctorId))
                .willReturn(25L);

            // When
            long count = appointmentService.countAppointmentsByDoctor(doctorId);

            // Then
            assertEquals(25L, count);
        }

        @Test
        @DisplayName("Tamamlanma orani hesaplama")
        void calculateCompletionRate() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            given(appointmentRepository.countByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, startDate, endDate)).willReturn(100L);
            given(appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, AppointmentStatus.COMPLETED, startDate, endDate)).willReturn(85L);

            // When
            double rate = appointmentService.calculateCompletionRate(doctorId, startDate, endDate);

            // Then
            assertEquals(85.0, rate);
        }

        @Test
        @DisplayName("Gelmeme orani hesaplama")
        void calculateNoShowRate() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            given(appointmentRepository.countByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, startDate, endDate)).willReturn(100L);
            given(appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, AppointmentStatus.NO_SHOW, startDate, endDate)).willReturn(5L);

            // When
            double rate = appointmentService.calculateNoShowRate(doctorId, startDate, endDate);

            // Then
            assertEquals(5.0, rate);
        }

        @Test
        @DisplayName("Randevu yoksa tamamlanma orani sifir")
        void calculateCompletionRate_RandevuYok() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            given(appointmentRepository.countByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, startDate, endDate)).willReturn(0L);

            // When
            double rate = appointmentService.calculateCompletionRate(doctorId, startDate, endDate);

            // Then
            assertEquals(0.0, rate);
        }
    }

    @Nested
    @DisplayName("Randevu Guncelleme Testleri")
    class RandevuGuncellemeTestleri {

        @Test
        @DisplayName("Randevu notlarini guncelleme")
        void updateAppointment_NotlarGuncelle() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));

            Appointment updateData = new Appointment();
            updateData.setDoctorNotes("Yeni doktor notlari");
            updateData.setPatientNotes("Yeni hasta notlari");

            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            Appointment result = appointmentService.updateAppointment(appointmentId, updateData);

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Randevu durumu guncelleme")
        void updateAppointmentStatus() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            Appointment result = appointmentService.updateAppointmentStatus(
                appointmentId, AppointmentStatus.NO_SHOW, "admin123", "Hasta gelmedi");

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }
    }

    @Nested
    @DisplayName("Randevu Silme Testleri")
    class RandevuSilmeTestleri {

        @Test
        @DisplayName("Randevu soft delete")
        void deleteAppointment() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            appointmentService.deleteAppointment(appointmentId, "admin123");

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }
    }

    @Nested
    @DisplayName("Hatirlatma Testleri")
    class HatirlatmaTestleri {

        @Test
        @DisplayName("Hatirlatma gonderme")
        void sendAppointmentReminder() {
            // Given
            given(appointmentRepository.findByIdAndDeletedFalse(appointmentId))
                .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class))).willReturn(testAppointment);

            // When
            appointmentService.sendAppointmentReminder(appointmentId);

            // Then
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }
    }
}
