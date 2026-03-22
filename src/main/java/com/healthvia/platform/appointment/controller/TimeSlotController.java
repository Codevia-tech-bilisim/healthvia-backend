package com.healthvia.platform.appointment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.service.TimeSlotService;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Slf4j
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    // === PUBLIC ENDPOINTS (Hastalar için müsait slotları görüntüleme) ===

    @GetMapping("/available")
    public ApiResponse<List<TimeSlot>> getAvailableSlots(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Getting available slots for doctor: {} on date: {}", doctorId, date);
        
        List<TimeSlot> slots = timeSlotService.findAvailableSlots(doctorId, date);
        return ApiResponse.success(slots);
    }

    @GetMapping("/available/range")
    public ApiResponse<List<TimeSlot>> getAvailableSlotsInRange(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting available slots for doctor: {} from {} to {}", doctorId, startDate, endDate);
        
        List<TimeSlot> slots = timeSlotService.findAvailableSlotsInRange(doctorId, startDate, endDate);
        return ApiResponse.success(slots);
    }

    // === DOCTOR ENDPOINTS (Doktorlar için slot yönetimi) ===

    @PostMapping("/generate")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<List<TimeSlot>> generateSlots(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "30") Integer durationMinutes) {
        
        // Güvenlik kontrolü: Sadece kendi slotlarını oluşturabilir
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(doctorId)) {
            throw new RuntimeException("Bu işlem için yetkiniz bulunmamaktadır");
        }
        
        log.info("Generating slots for doctor: {} on date: {} with duration: {}", 
                doctorId, date, durationMinutes);
        
        List<TimeSlot> slots = timeSlotService.generateSlotsForDay(doctorId, date, durationMinutes);
        return ApiResponse.success(slots, "Slotlar başarıyla oluşturuldu");
    }

    @PostMapping("/generate/week")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<List<TimeSlot>> generateWeeklySlots(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "30") Integer durationMinutes) {
        
        // Güvenlik kontrolü
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(doctorId)) {
            throw new RuntimeException("Bu işlem için yetkiniz bulunmamaktadır");
        }
        
        log.info("Generating weekly slots for doctor: {} starting from: {}", doctorId, startDate);
        
        List<TimeSlot> slots = timeSlotService.generateSlotsForWeek(doctorId, startDate, durationMinutes);
        return ApiResponse.success(slots, "Haftalık slotlar oluşturuldu");
    }

    @PostMapping("/generate/month")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<List<TimeSlot>> generateMonthlySlots(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "30") Integer durationMinutes) {
        
        // Güvenlik kontrolü
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(doctorId)) {
            throw new RuntimeException("Bu işlem için yetkiniz bulunmamaktadır");
        }
        
        log.info("Generating monthly slots for doctor: {} starting from: {}", doctorId, startDate);
        
        List<TimeSlot> slots = timeSlotService.generateSlotsForMonth(doctorId, startDate, durationMinutes);
        return ApiResponse.success(slots, "Aylık slotlar oluşturuldu");
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ApiResponse<List<TimeSlot>> getDoctorSlots(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Doktor sadece kendi slotlarını görebilir
        if (SecurityUtils.hasRole(com.healthvia.platform.common.enums.UserRole.DOCTOR)) {
            String currentUserId = SecurityUtils.getCurrentUserId();
            if (!currentUserId.equals(doctorId)) {
                throw new RuntimeException("Bu işlem için yetkiniz bulunmamaktadır");
            }
        }
        
        List<TimeSlot> slots = timeSlotService.findSlotsByDoctor(doctorId, startDate, endDate);
        return ApiResponse.success(slots);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ApiResponse<TimeSlot> blockSlot(
            @PathVariable String id,
            @RequestParam String reason,
            @RequestParam String blockedBy) {
        
        log.info("Blocking slot: {} by: {} reason: {}", id, blockedBy, reason);
        
        TimeSlot slot = timeSlotService.blockSlot(id, reason, blockedBy);
        return ApiResponse.success(slot, "Slot bloklandı");
    }

    @PatchMapping("/{id}/unblock")
    public ApiResponse<TimeSlot> unblockSlot(@PathVariable String id) {
        
        log.info("Unblocking slot: {}", id);
        
        TimeSlot slot = timeSlotService.unblockSlot(id);
        return ApiResponse.success(slot, "Slot bloklaması kaldırıldı");
    }

    // === ADMIN ENDPOINTS (Admin için slot yönetimi) ===

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ApiResponse<Void> deleteSlot(
            @PathVariable String id,
            @RequestParam String deletedBy) {
        
        log.info("Deleting slot: {} by: {}", id, deletedBy);
        
        timeSlotService.deleteSlot(id, deletedBy);
        return ApiResponse.success("Slot silindi");
    }

    @PostMapping("/cleanup-expired")
    public ApiResponse<Void> cleanupExpiredSlots() {
        log.info("Cleaning up expired slots");
        
        timeSlotService.expireOldSlots();
        return ApiResponse.success("Eski slotlar temizlendi");
    }

    // === UTILITY ENDPOINTS (Genel kullanım) ===

    @GetMapping("/{id}")
    public ApiResponse<TimeSlot> getSlot(@PathVariable String id) {
        TimeSlot slot = timeSlotService.findById(id)
            .orElseThrow(() -> new RuntimeException("Slot bulunamadı: " + id));
        
        return ApiResponse.success(slot);
    }

    @GetMapping("/check-availability")
    public ApiResponse<Boolean> checkSlotAvailability(@RequestParam String slotId) {
        boolean available = timeSlotService.isSlotAvailable(slotId);
        return ApiResponse.success(available);
    }

    @GetMapping("/count-available")
    public ApiResponse<Integer> countAvailableSlots(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        int count = timeSlotService.countAvailableSlots(doctorId, date);
        return ApiResponse.success(count);
    }
}