package com.healthvia.platform.appointment.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.entity.TimeSlot.SlotStatus;

public interface TimeSlotRepository extends MongoRepository<TimeSlot, String> {

    // === TEMEL SORGULAR ===
    Optional<TimeSlot> findByIdAndDeletedFalse(String id);
    
    List<TimeSlot> findByDoctorIdAndDateAndDeletedFalse(String doctorId, LocalDate date);
    
    List<TimeSlot> findByDoctorIdAndDateBetweenAndDeletedFalse(
        String doctorId, LocalDate startDate, LocalDate endDate);

    // === DURUM SORGULARI ===
    List<TimeSlot> findByDoctorIdAndDateAndStatusAndDeletedFalse(
        String doctorId, LocalDate date, SlotStatus status);
    
    List<TimeSlot> findByDoctorIdAndStatusAndDeletedFalse(String doctorId, SlotStatus status);

    // === MÜSAİT SLOT SORGULARI ===
    @Query("{'doctorId': ?0, 'date': ?1, 'status': 'AVAILABLE', 'deleted': false}")
    List<TimeSlot> findAvailableSlots(String doctorId, LocalDate date);

    @Query("{'doctorId': ?0, 'date': {'$gte': ?1, '$lte': ?2}, " +
           "'status': 'AVAILABLE', 'deleted': false}")
    List<TimeSlot> findAvailableSlotsInRange(String doctorId, LocalDate startDate, LocalDate endDate);

    // === SAYMA İŞLEMLERİ ===
    long countByDoctorIdAndDateAndStatusAndDeletedFalse(
        String doctorId, LocalDate date, SlotStatus status);
    
    @Query(value = "{'doctorId': ?0, 'date': ?1, 'status': 'AVAILABLE', 'deleted': false}",
           count = true)
    long countAvailableSlots(String doctorId, LocalDate date);

    // === VARLIK KONTROLLERI ===
    boolean existsByDoctorIdAndDateAndDeletedFalse(String doctorId, LocalDate date);

    // === ZAMAN ÇAKIŞMA SORGULARI ===
    @Query("{'doctorId': ?0, 'date': ?1, 'deleted': false, " +
           "'$or': [" +
           "  {'startTime': {'$lt': ?3}, 'endTime': {'$gt': ?2}}, " +
           "  {'startTime': {'$gte': ?2, '$lt': ?3}}, " +
           "  {'endTime': {'$gt': ?2, '$lte': ?3}}" +
           "]}")
    List<TimeSlot> findOverlappingSlots(String doctorId, LocalDate date,
                                       LocalTime startTime, LocalTime endTime);

    // === RANDEVU İLE SLOT BULMA ===
    Optional<TimeSlot> findByAppointmentIdAndDeletedFalse(String appointmentId);

    // === TEMİZLEME SORGULARI ===
    List<TimeSlot> findByDateBeforeAndDeletedFalse(LocalDate date);
    
    @Query("{'date': {'$lt': ?0}, 'status': {'$in': ['AVAILABLE', 'EXPIRED']}, 'deleted': false}")
    List<TimeSlot> findExpiredSlots(LocalDate beforeDate);
}