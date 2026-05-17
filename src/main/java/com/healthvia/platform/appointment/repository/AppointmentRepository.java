package com.healthvia.platform.appointment.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.entity.Appointment.AppointmentStatus;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    // === TEMEL SORGULAR ===
    Optional<Appointment> findByIdAndDeletedFalse(String id);
    
    Page<Appointment> findByPatientIdAndDeletedFalse(String patientId, Pageable pageable);
    
    Page<Appointment> findByDoctorIdAndDeletedFalse(String doctorId, Pageable pageable);
    
    List<Appointment> findByDoctorIdAndAppointmentDateAndDeletedFalse(String doctorId, LocalDate date);

    // === DURUM SORGULARI ===
    Page<Appointment> findByStatusAndDeletedFalse(AppointmentStatus status, Pageable pageable);
    
    long countByStatusAndDeletedFalse(AppointmentStatus status);
    
    long countByDoctorIdAndStatusAndDeletedFalse(String doctorId, AppointmentStatus status);

    // === TARİH ARALIĞI SORGULARI ===
    // Explicit $gte/$lte: Spring Data Mongo's derived `Between` keyword is
    // exclusive ($gt/$lt), so appointments on the boundary dates were dropped.
    @Query("{'appointmentDate': {'$gte': ?0, '$lte': ?1}, 'deleted': false}")
    List<Appointment> findByAppointmentDateBetweenAndDeletedFalse(LocalDate startDate, LocalDate endDate);

    @Query("{'doctorId': ?0, 'appointmentDate': {'$gte': ?1, '$lte': ?2}, 'deleted': false}")
    List<Appointment> findByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(
        String doctorId, LocalDate startDate, LocalDate endDate);

    @Query("{'patientId': ?0, 'appointmentDate': {'$gte': ?1, '$lte': ?2}, 'deleted': false}")
    List<Appointment> findByPatientIdAndAppointmentDateBetweenAndDeletedFalse(
        String patientId, LocalDate startDate, LocalDate endDate);

    // === İSTATİSTİKLER ===
    long countByDoctorIdAndDeletedFalse(String doctorId);
    
    long countByPatientIdAndDeletedFalse(String patientId);
    
    @Query(value = "{'doctorId': ?0, 'appointmentDate': {'$gte': ?1, '$lte': ?2}, 'deleted': false}",
           count = true)
    long countByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(
        String doctorId, LocalDate startDate, LocalDate endDate);

    @Query(value = "{'doctorId': ?0, 'status': ?1, " +
           "'appointmentDate': {'$gte': ?2, '$lte': ?3}, 'deleted': false}",
           count = true)
    long countByDoctorIdAndStatusAndAppointmentDateBetweenAndDeletedFalse(
        String doctorId, AppointmentStatus status, LocalDate startDate, LocalDate endDate);

    // === ÇAKIŞMA TESPİTİ ===
    @Query("{'doctorId': ?0, 'appointmentDate': ?1, 'deleted': false, " +
           "'status': {'$nin': ['CANCELLED', 'NO_SHOW', 'RESCHEDULED']}, " +
           "'$or': [" +
           "  {'startTime': {'$lt': ?3}, 'endTime': {'$gt': ?2}}, " +
           "  {'startTime': {'$gte': ?2, '$lt': ?3}}, " +
           "  {'endTime': {'$gt': ?2, '$lte': ?3}}" +
           "]}")
    List<Appointment> findConflictingAppointments(String doctorId, LocalDate date,
                                                 LocalTime startTime, LocalTime endTime);

    // === YAKLAŞAN RANDEVULAR ===
    @Query("{'$or': [{'patientId': ?0}, {'doctorId': ?0}], " +
           "'appointmentDate': {'$gte': ?1, '$lte': ?2}, " +
           "'status': {'$nin': ['CANCELLED', 'NO_SHOW', 'RESCHEDULED']}, " +
           "'deleted': false}")
    List<Appointment> findUpcomingAppointments(String userId, LocalDate startDate, LocalDate endDate);

    // === BUGÜNÜN RANDEVULARI ===
    @Query("{'doctorId': ?0, 'appointmentDate': ?1, 'deleted': false, " +
           "'status': {'$nin': ['CANCELLED', 'NO_SHOW', 'RESCHEDULED']}}")
    List<Appointment> findTodayAppointments(String doctorId, LocalDate today);

    // === HATIRLATMA SORGULARI ===
    @Query("{'appointmentDate': {'$gte': ?0, '$lte': ?1}, " +
           "'status': {'$in': ['PENDING', 'CONFIRMED']}, " +
           "'reminderSentAt': null, " +
           "'deleted': false}")
    List<Appointment> findAppointmentsForReminder(LocalDate startDate, LocalDate endDate);

    // === ÖDEME DURUMU ===
    List<Appointment> findByPaymentStatusAndDeletedFalse(Appointment.PaymentStatus paymentStatus);
    
    long countByPaymentStatusAndDeletedFalse(Appointment.PaymentStatus paymentStatus);
}