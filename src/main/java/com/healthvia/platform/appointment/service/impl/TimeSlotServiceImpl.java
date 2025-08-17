package com.healthvia.platform.appointment.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.entity.TimeSlot.SlotStatus;
import com.healthvia.platform.appointment.repository.TimeSlotRepository;
import com.healthvia.platform.appointment.service.TimeSlotService;
import com.healthvia.platform.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    // === TEMEL CRUD İŞLEMLERİ ===

    @Override
    public TimeSlot createSlot(TimeSlot slot) {
        return timeSlotRepository.save(slot);
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
        if (!existingSlots.isEmpty()) {
            log.warn("Slots already exist for doctor {} on date {}", doctorId, date);
            return existingSlots;
        }

        // Basit slot oluşturma - 09:00'dan 17:00'a kadar
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime currentTime = LocalTime.of(9, 0); // 09:00
        LocalTime endTime = LocalTime.of(17, 0);    // 17:00

        while (currentTime.plusMinutes(durationMinutes).isBefore(endTime) || 
               currentTime.plusMinutes(durationMinutes).equals(endTime)) {
            
            LocalTime slotEnd = currentTime.plusMinutes(durationMinutes);
            
            TimeSlot slot = TimeSlot.builder()
                .doctorId(doctorId)
                .date(date)
                .startTime(currentTime)
                .endTime(slotEnd)
                .durationMinutes(durationMinutes)
                .status(SlotStatus.AVAILABLE)
                .isRecurring(false)
                .build();
            
            slots.add(slot);
            currentTime = currentTime.plusMinutes(durationMinutes + 5); // 5 dk buffer
        }

        // Slotları kaydet
        List<TimeSlot> savedSlots = timeSlotRepository.saveAll(slots);
        log.info("Generated {} slots for doctor {} on {}", savedSlots.size(), doctorId, date);
        
        return savedSlots;
    }

    @Override
    public List<TimeSlot> generateSlotsForWeek(String doctorId, LocalDate startDate, Integer durationMinutes) {
        log.info("Generating weekly slots for doctor: {} starting from: {}", doctorId, startDate);
        
        List<TimeSlot> allSlots = new ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            
            // Hafta sonu kontrolü - sadece hafta içi slot oluştur
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                List<TimeSlot> dailySlots = generateSlotsForDay(doctorId, currentDate, durationMinutes);
                allSlots.addAll(dailySlots);
            }
        }
        
        return allSlots;
    }

    @Override
    public List<TimeSlot> generateSlotsForMonth(String doctorId, LocalDate startDate, Integer durationMinutes) {
        log.info("Generating monthly slots for doctor: {} starting from: {}", doctorId, startDate);
        
        List<TimeSlot> allSlots = new ArrayList<>();
        LocalDate endDate = startDate.plusMonths(1);
        
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                List<TimeSlot> dailySlots = generateSlotsForDay(doctorId, currentDate, durationMinutes);
                allSlots.addAll(dailySlots);
            }
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
        return timeSlotRepository.findAvailableSlots(doctorId, date);
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