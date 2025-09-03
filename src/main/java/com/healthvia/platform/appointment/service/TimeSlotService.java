package com.healthvia.platform.appointment.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.entity.TimeSlot.SlotStatus;

public interface TimeSlotService {

    // === TEMEL CRUD İŞLEMLERİ ===
    
    /**
     * Slot oluştur
     */
    TimeSlot createSlot(String doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, Integer durationMinutes);
    
    /**
     * Slot güncelle
     */
    TimeSlot updateSlot(String id, TimeSlot slot);
    
    /**
     * Slot bul (ID ile)
     */
    Optional<TimeSlot> findById(String id);
    
    /**
     * Slot sil (soft delete)
     */
    void deleteSlot(String id, String deletedBy);

    // === SLOT OLUŞTURMA ===
    
    /**
     * Bir günlük slotlar oluştur
     */
    List<TimeSlot> generateSlotsForDay(String doctorId, LocalDate date, Integer durationMinutes);
    
    /**
     * Bir haftalık slotlar oluştur
     */
    List<TimeSlot> generateSlotsForWeek(String doctorId, LocalDate startDate, Integer durationMinutes);
    
    /**
     * Bir aylık slotlar oluştur
     */
    List<TimeSlot> generateSlotsForMonth(String doctorId, LocalDate startDate, Integer durationMinutes);

    // === SLOT YÖNETİMİ ===
    
    /**
     * Slot'u rezerve et (randevu alındığında)
     */
    TimeSlot bookSlot(String slotId, String appointmentId);
    
    /**
     * Slot'u serbest bırak (randevu iptal edildiğinde)
     */
    TimeSlot releaseSlot(String slotId);
    
    /**
     * Slot'u blokla (doktor müsait değil)
     */
    TimeSlot blockSlot(String slotId, String reason, String blockedBy);
    
    /**
     * Slot bloğunu kaldır
     */
    TimeSlot unblockSlot(String slotId);

    // === SORGULAMA İŞLEMLERİ ===
    
    /**
     * Müsait slotları getir (belirli gün)
     */
    List<TimeSlot> findAvailableSlots(String doctorId, LocalDate date);
    
    /**
     * Müsait slotları getir (tarih aralığı)
     */
    List<TimeSlot> findAvailableSlotsInRange(String doctorId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Doktorun tüm slotlarını getir
     */
    List<TimeSlot> findSlotsByDoctor(String doctorId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Duruma göre slotları getir
     */
    List<TimeSlot> findSlotsByStatus(String doctorId, SlotStatus status, LocalDate date);
    
    // === MÜSAİTLİK KONTROLLERI ===
    
    /**
     * Slot müsait mi?
     */
    boolean isSlotAvailable(String slotId);
    
    /**
     * O günde slot var mı?
     */
    boolean hasSlotsOnDate(String doctorId, LocalDate date);
    
    /**
     * Müsait slot sayısı
     */
    int countAvailableSlots(String doctorId, LocalDate date);

    // === TEMİZLEME İŞLEMLERİ ===
    
    /**
     * Eski slotların süresini doldurumuş olarak işaretle
     */
    void expireOldSlots();
    
    /**
     * Belirli tarihten önceki slotları temizle
     */
    void cleanupExpiredSlots(LocalDate beforeDate);
}