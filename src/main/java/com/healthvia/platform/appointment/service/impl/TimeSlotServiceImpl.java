package com.healthvia.platform.appointment.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.entity.TimeSlot.SlotStatus;
import com.healthvia.platform.appointment.repository.TimeSlotRepository;
import com.healthvia.platform.appointment.service.TimeSlotService;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.doctor.entity.Doctor;
import com.healthvia.platform.doctor.service.DoctorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorService doctorService;

    // === TEMEL CRUD İŞLEMLERİ ===

    public TimeSlot createSlot(String doctorId, LocalDate date, LocalTime startTime, 
                               LocalTime endTime, Integer durationMinutes) {
        return TimeSlot.builder()
            .doctorId(doctorId)
            .date(date)
            .startTime(startTime)
            .endTime(endTime)
            .durationMinutes(durationMinutes)
            .status(SlotStatus.AVAILABLE)
            .isRecurring(false)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimeSlot> findById(String id) {
        return timeSlotRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public TimeSlot updateSlot(String id, TimeSlot slot) {
        TimeSlot existingSlot = findByIdOrThrow(id);

        // Güvenli güncelleme alanları
        if (slot.getStatus() != null) existingSlot.setStatus(slot.getStatus());
        if (slot.getAppointmentId() != null) existingSlot.setAppointmentId(slot.getAppointmentId());
        if (slot.getStartTime() != null) existingSlot.setStartTime(slot.getStartTime());
        if (slot.getEndTime() != null) existingSlot.setEndTime(slot.getEndTime());
        if (slot.getDurationMinutes() != null) existingSlot.setDurationMinutes(slot.getDurationMinutes());
        if (slot.getAllowedConsultationTypes() != null) {
            existingSlot.setAllowedConsultationTypes(slot.getAllowedConsultationTypes());
        }

        return timeSlotRepository.save(existingSlot);
    }

    @Override
    public void deleteSlot(String id, String deletedBy) {
        TimeSlot slot = findByIdOrThrow(id);
        slot.markAsDeleted(deletedBy);
        timeSlotRepository.save(slot);
    }

    // === SLOT OLUŞTURMA ===

    @Override
    public List<TimeSlot> generateSlotsForDay(String doctorId, LocalDate date, Integer durationMinutes) {
        log.info("Generating slots for doctor: {} on date: {} with duration: {}",
                doctorId, date, durationMinutes);

        // Mevcut slotları kontrol et
        List<TimeSlot> existingSlots = timeSlotRepository.findByDoctorIdAndDateAndDeletedFalse(doctorId, date);

        // Sadece aktif (AVAILABLE veya BOOKED) slotlar varsa skip et
        List<TimeSlot> activeSlots = existingSlots.stream()
            .filter(s -> s.getStatus() == SlotStatus.AVAILABLE || s.getStatus() == SlotStatus.BOOKED)
            .toList();
        if (!activeSlots.isEmpty()) {
            log.warn("Active slots already exist for doctor {} on date {}", doctorId, date);
            return existingSlots;
        }

        // EXPIRED slotlar varsa temizle
        if (!existingSlots.isEmpty()) {
            log.info("Cleaning up {} expired/inactive slots for doctor {} on date {}", existingSlots.size(), doctorId, date);
            existingSlots.forEach(s -> s.markAsDeleted("REGENERATE"));
            timeSlotRepository.saveAll(existingSlots);
        }

        // Doktor çalışma saatlerini DB'den oku
        Doctor doctor = doctorService.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        LocalTime workStart = doctor.getWorkingHoursStart() != null ? doctor.getWorkingHoursStart() : LocalTime.of(9, 0);
        LocalTime workEnd = doctor.getWorkingHoursEnd() != null ? doctor.getWorkingHoursEnd() : LocalTime.of(17, 0);
        LocalTime lunchStart = doctor.getLunchBreakStart() != null ? doctor.getLunchBreakStart() : LocalTime.of(12, 0);
        LocalTime lunchEnd = doctor.getLunchBreakEnd() != null ? doctor.getLunchBreakEnd() : LocalTime.of(13, 0);
        int bufferMinutes = doctor.getAppointmentBufferMinutes() != null ? doctor.getAppointmentBufferMinutes() : 5;

        List<TimeSlot> slots = new ArrayList<>();

        // Sabah slotları (workStart - lunchStart)
        LocalTime currentTime = workStart;
        while (currentTime.plusMinutes(durationMinutes).isBefore(lunchStart) ||
               currentTime.plusMinutes(durationMinutes).equals(lunchStart)) {

            LocalTime slotEnd = currentTime.plusMinutes(durationMinutes);
            slots.add(createSlot(doctorId, date, currentTime, slotEnd, durationMinutes));
            currentTime = currentTime.plusMinutes(durationMinutes + bufferMinutes);
        }

        // Öğleden sonra slotları (lunchEnd - workEnd)
        currentTime = lunchEnd;
        while (currentTime.plusMinutes(durationMinutes).isBefore(workEnd) ||
               currentTime.plusMinutes(durationMinutes).equals(workEnd)) {

            LocalTime slotEnd = currentTime.plusMinutes(durationMinutes);
            slots.add(createSlot(doctorId, date, currentTime, slotEnd, durationMinutes));
            currentTime = currentTime.plusMinutes(durationMinutes + bufferMinutes);
        }

        // Slotları kaydet
        List<TimeSlot> savedSlots = timeSlotRepository.saveAll(slots);
        log.info("Generated {} slots for doctor {} on {} (morning: {}, afternoon: {})",
                savedSlots.size(), doctorId, date,
                savedSlots.stream().filter(s -> s.getStartTime().isBefore(lunchStart)).count(),
                savedSlots.stream().filter(s -> s.getStartTime().isAfter(lunchStart)).count());

        return savedSlots;
    }

    @Override
    public List<TimeSlot> generateSlotsForWeek(String doctorId, LocalDate startDate, Integer durationMinutes) {
        log.info("Generating weekly slots for doctor: {} starting from: {}", doctorId, startDate);

        Doctor doctor = doctorService.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Set<String> workingDays = doctor.getWorkingDays();

        List<TimeSlot> allSlots = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            String dayName = currentDate.getDayOfWeek().name(); // MONDAY, TUESDAY, ...

            if (workingDays != null && !workingDays.isEmpty()) {
                if (!workingDays.contains(dayName)) continue;
            } else {
                // Fallback: hafta sonu hariç
                DayOfWeek dow = currentDate.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
            }

            List<TimeSlot> dailySlots = generateSlotsForDay(doctorId, currentDate, durationMinutes);
            allSlots.addAll(dailySlots);
        }

        return allSlots;
    }

    @Override
    public List<TimeSlot> generateSlotsForMonth(String doctorId, LocalDate startDate, Integer durationMinutes) {
        log.info("Generating monthly slots for doctor: {} starting from: {}", doctorId, startDate);

        Doctor doctor = doctorService.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Set<String> workingDays = doctor.getWorkingDays();

        List<TimeSlot> allSlots = new ArrayList<>();
        LocalDate endDate = startDate.plusMonths(1);

        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            String dayName = currentDate.getDayOfWeek().name();

            if (workingDays != null && !workingDays.isEmpty()) {
                if (!workingDays.contains(dayName)) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
            } else {
                DayOfWeek dow = currentDate.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
            }

            List<TimeSlot> dailySlots = generateSlotsForDay(doctorId, currentDate, durationMinutes);
            allSlots.addAll(dailySlots);
            currentDate = currentDate.plusDays(1);
        }

        return allSlots;
    }

    // === SLOT YÖNETİMİ ===

    @Override
    public TimeSlot bookSlot(String slotId, String appointmentId) {
        log.info("Booking slot: {} for appointment: {}", slotId, appointmentId);
        
        TimeSlot slot = findByIdOrThrow(slotId);
        
        if (!slot.isAvailable()) {
            throw new RuntimeException("Bu slot artık müsait değil");
        }
        
        slot.book(appointmentId);
        return timeSlotRepository.save(slot);
    }

    @Override
    public TimeSlot releaseSlot(String slotId) {
        log.info("Releasing slot: {}", slotId);
        
        TimeSlot slot = findByIdOrThrow(slotId);
        slot.makeAvailable();
        
        return timeSlotRepository.save(slot);
    }

    @Override
    public TimeSlot blockSlot(String slotId, String reason, String blockedBy) {
        log.info("Blocking slot: {} by: {} reason: {}", slotId, blockedBy, reason);
        
        TimeSlot slot = findByIdOrThrow(slotId);
        slot.block(reason, blockedBy);
        
        return timeSlotRepository.save(slot);
    }

    @Override
    public TimeSlot unblockSlot(String slotId) {
        TimeSlot slot = findByIdOrThrow(slotId);
        
        if (!slot.getStatus().equals(SlotStatus.BLOCKED)) {
            throw new RuntimeException("Bu slot bloklanmış durumda değil");
        }
        
        slot.makeAvailable();
        return timeSlotRepository.save(slot);
    }

    // === SORGULAMA İŞLEMLERİ ===

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> findAvailableSlots(String doctorId, LocalDate date) {
        return timeSlotRepository.findByDoctorIdAndDateAndStatusAndDeletedFalse(
                doctorId, date, TimeSlot.SlotStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> findAvailableSlotsInRange(String doctorId, LocalDate startDate, LocalDate endDate) {
        return timeSlotRepository.findAvailableSlotsInRange(doctorId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> findSlotsByDoctor(String doctorId, LocalDate startDate, LocalDate endDate) {
        return timeSlotRepository.findByDoctorIdAndDateBetweenAndDeletedFalse(doctorId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> findSlotsByStatus(String doctorId, SlotStatus status, LocalDate date) {
        return timeSlotRepository.findByDoctorIdAndDateAndStatusAndDeletedFalse(doctorId, date, status);
    }

    // === MÜSAİTLİK KONTROLLERI ===

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(String slotId) {
        return timeSlotRepository.findByIdAndDeletedFalse(slotId)
            .map(TimeSlot::isAvailable)
            .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSlotsOnDate(String doctorId, LocalDate date) {
        return timeSlotRepository.existsByDoctorIdAndDateAndDeletedFalse(doctorId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public int countAvailableSlots(String doctorId, LocalDate date) {
        return (int) timeSlotRepository.countAvailableSlots(doctorId, date);
    }

    // === TEMİZLEME İŞLEMLERİ ===

    @Override
    public void expireOldSlots() {
        log.info("Expiring old slots before today");
        
        List<TimeSlot> expiredSlots = timeSlotRepository.findExpiredSlots(LocalDate.now());
        
        for (TimeSlot slot : expiredSlots) {
            if (slot.getStatus().equals(SlotStatus.AVAILABLE)) {
                slot.setStatus(SlotStatus.EXPIRED);
            }
        }
        
        timeSlotRepository.saveAll(expiredSlots);
        log.info("Expired {} old slots", expiredSlots.size());
    }

    @Override
    public void cleanupExpiredSlots(LocalDate beforeDate) {
        log.info("Cleaning up expired slots before: {}", beforeDate);
        
        List<TimeSlot> expiredSlots = timeSlotRepository.findByDateBeforeAndDeletedFalse(beforeDate);
        
        for (TimeSlot slot : expiredSlots) {
            slot.markAsDeleted("SYSTEM_CLEANUP");
        }
        
        timeSlotRepository.saveAll(expiredSlots);
        log.info("Cleaned up {} expired slots", expiredSlots.size());
    }

    // === HELPER METHODS ===

    private TimeSlot findByIdOrThrow(String id) {
        return timeSlotRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Slot bulunamadı: " + id));
    }
}