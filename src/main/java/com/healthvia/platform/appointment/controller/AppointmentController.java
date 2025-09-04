package com.healthvia.platform.appointment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.appointment.dto.AppointmentStatisticsDto;
import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.entity.Appointment.AppointmentStatus;
import com.healthvia.platform.appointment.service.AppointmentService;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.ValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    // === HASTA ENDPOİNTLERİ ===

    @PostMapping
    public ApiResponse<Appointment> createAppointment(
            @RequestParam String patientId,
            @RequestParam String doctorId,
            @RequestParam String slotId,
            @RequestParam(required = false) String treatmentTypeId,
            @RequestParam(required = false) String chiefComplaint) {
        
        log.info("Patient {} creating appointment with doctor {}", patientId, doctorId);

        String sanitizedComplaint = ValidationUtils.sanitizeChiefComplaint(chiefComplaint);
        
        Appointment appointment = appointmentService.bookAppointment(
            patientId,
            doctorId,
            slotId,
            treatmentTypeId,
            sanitizedComplaint
        );

        return ApiResponse.success(appointment, "Randevu başarıyla oluşturuldu");
    }

    @GetMapping("/patient/{patientId}")
    public ApiResponse<Page<Appointment>> getPatientAppointments(
            @PathVariable String patientId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Appointment> appointments = appointmentService.findByPatientId(patientId, pageable);
        return ApiResponse.success(appointments);
    }

    @GetMapping("/{id}")
    public ApiResponse<Appointment> getAppointment(@PathVariable String id) {
        
        Appointment appointment = appointmentService.findById(id)
            .orElseThrow(() -> new RuntimeException("Randevu bulunamadı: " + id));

        return ApiResponse.success(appointment);
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<Appointment> cancelAppointment(
            @PathVariable String id,
            @RequestParam String cancelledBy,
            @RequestParam(required = false) String reason) {
        
        Appointment appointment = appointmentService.cancelAppointment(id, cancelledBy, reason);
        return ApiResponse.success(appointment, "Randevu başarıyla iptal edildi");
    }

    // === DOKTOR ENDPOİNTLERİ ===

    @GetMapping("/doctor/{doctorId}/today")
    public ApiResponse<List<Appointment>> getTodayAppointments(@PathVariable String doctorId) {
        
        List<Appointment> appointments = appointmentService.findTodayAppointments(doctorId);
        return ApiResponse.success(appointments);
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<Page<Appointment>> getDoctorAppointments(
            @PathVariable String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Appointment> appointments;
        
        if (date != null) {
            List<Appointment> dailyAppointments = appointmentService.findByDoctorAndDate(doctorId, date);
            appointments = new org.springframework.data.domain.PageImpl<>(
                dailyAppointments, pageable, dailyAppointments.size());
        } else {
            appointments = appointmentService.findByDoctorId(doctorId, pageable);
        }
        
        return ApiResponse.success(appointments);
    }

    @PatchMapping("/{id}/confirm")
    public ApiResponse<Appointment> confirmAppointment(
            @PathVariable String id,
            @RequestParam String confirmedBy) {
        
        Appointment appointment = appointmentService.confirmAppointment(id, confirmedBy);
        return ApiResponse.success(appointment, "Randevu onaylandı");
    }

    @PatchMapping("/{id}/start")
    public ApiResponse<Appointment> startConsultation(
            @PathVariable String id,
            @RequestParam String doctorId) {
        
        Appointment appointment = appointmentService.startConsultation(id, doctorId);
        return ApiResponse.success(appointment, "Muayene başlatıldı");
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<Appointment> completeAppointment(
            @PathVariable String id,
            @RequestParam String doctorId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String prescriptionId) {
        
        Appointment appointment = appointmentService.completeAppointment(id, doctorId, notes, prescriptionId);
        return ApiResponse.success(appointment, "Randevu tamamlandı");
    }

    @PatchMapping("/{id}/check-in")
    public ApiResponse<Appointment> checkInPatient(@PathVariable String id) {
        
        Appointment appointment = appointmentService.checkInPatient(id);
        return ApiResponse.success(appointment, "Hasta check-in yapıldı");
    }

    // === ADMIN ENDPOİNTLERİ ===

    @GetMapping("/admin/all")
    public ApiResponse<Page<Appointment>> getAllAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Appointment> appointments;
        
        if (status != null) {
            appointments = appointmentService.findByStatus(status, pageable);
        } else {
            appointments = appointmentService.findAll(pageable);
        }
        
        return ApiResponse.success(appointments);
    }

    @GetMapping("/admin/statistics")
    public ApiResponse<AppointmentStatisticsDto> getAppointmentStatistics(
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Builder pattern ile temiz kod
        AppointmentStatisticsDto.AppointmentStatisticsDtoBuilder statsBuilder = AppointmentStatisticsDto.builder();
        
        // Temel istatistikler
        long pending = appointmentService.countAppointmentsByStatus(AppointmentStatus.PENDING);
        long confirmed = appointmentService.countAppointmentsByStatus(AppointmentStatus.CONFIRMED);
        long completed = appointmentService.countAppointmentsByStatus(AppointmentStatus.COMPLETED);
        long cancelled = appointmentService.countAppointmentsByStatus(AppointmentStatus.CANCELLED);
        
        statsBuilder
            .totalAppointments(pending + confirmed + completed)
            .pendingAppointments(pending)
            .confirmedAppointments(confirmed)
            .completedAppointments(completed)
            .cancelledAppointments(cancelled);
        
        // Doktor bazlı istatistikler (opsiyonel)
        if (doctorId != null && startDate != null && endDate != null) {
            double completionRate = appointmentService.calculateCompletionRate(doctorId, startDate, endDate);
            double noShowRate = appointmentService.calculateNoShowRate(doctorId, startDate, endDate);
            
            statsBuilder
                .completionRate(completionRate)
                .noShowRate(noShowRate);
        }
        
        return ApiResponse.success(statsBuilder.build());
    }

    // === GENEL ENDPOİNTLER ===

    @GetMapping("/upcoming/{userId}")
    public ApiResponse<List<Appointment>> getUpcomingAppointments(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int days) {
        
        List<Appointment> appointments = appointmentService.findUpcomingAppointments(userId, days);
        return ApiResponse.success(appointments);
    }

    @PostMapping("/{id}/send-reminder")
    public ApiResponse<Void> sendAppointmentReminder(@PathVariable String id) {
        appointmentService.sendAppointmentReminder(id);
        return ApiResponse.success("Hatırlatma gönderildi");
    }
}