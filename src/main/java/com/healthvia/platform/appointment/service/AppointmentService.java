package com.healthvia.platform.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.entity.Appointment.AppointmentStatus;

public interface AppointmentService {

    // === TEMEL CRUD İŞLEMLERİ ===
    
    /**
     * Randevu oluştur
     */
    Appointment createAppointment(Appointment appointment);
    
    /**
     * Randevu güncelle
     */
    Appointment updateAppointment(String id, Appointment appointment);
    
    /**
     * Randevu bul (ID ile)
     */
    Optional<Appointment> findById(String id);
    
    /**
     * Randevu sil (soft delete)
     */
    void deleteAppointment(String id, String deletedBy);
    
    /**
     * Tüm randevuları listele (sayfalı)
     */
    Page<Appointment> findAll(Pageable pageable);

    // === RANDEVU ALMA İŞLEMLERİ ===
    
    /**
     * Randevu al - Ana metod
     */
    Appointment bookAppointment(String patientId, String doctorId, String slotId, 
                               String treatmentTypeId, String chiefComplaint);
    
    /**
     * Randevuyu onayla (doktor tarafından)
     */
    Appointment confirmAppointment(String appointmentId, String confirmedBy);
    
    /**
     * Randevuyu iptal et
     */
    Appointment cancelAppointment(String appointmentId, String cancelledBy, String reason);
    
    /**
     * Randevuyu ertele
     */
    Appointment rescheduleAppointment(String appointmentId, String newSlotId, String rescheduledBy);

    // === DURUM YÖNETİMİ ===
    
    /**
     * Randevu durumunu güncelle
     */
    Appointment updateAppointmentStatus(String appointmentId, AppointmentStatus status, 
                                       String updatedBy, String reason);
    
    /**
     * Hasta check-in işlemi
     */
    Appointment checkInPatient(String appointmentId);
    
    /**
     * Muayeneyi başlat
     */
    Appointment startConsultation(String appointmentId, String doctorId);
    
    /**
     * Muayeneyi tamamla
     */
    Appointment completeAppointment(String appointmentId, String doctorId, 
                                   String notes, String prescriptionId);

    // === SORGULAMA İŞLEMLERİ ===
    
    /**
     * Hastanın randevularını getir
     */
    Page<Appointment> findByPatientId(String patientId, Pageable pageable);
    
    /**
     * Doktorun randevularını getir
     */
    Page<Appointment> findByDoctorId(String doctorId, Pageable pageable);
    
    /**
     * Duruma göre randevuları getir
     */
    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);
    
    /**
     * Tarih aralığındaki randevuları getir
     */
    List<Appointment> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Doktorun belirli gündeki randevuları
     */
    List<Appointment> findByDoctorAndDate(String doctorId, LocalDate date);
    
    /**
     * Hastanın tarih aralığındaki randevuları
     */
    List<Appointment> findByPatientAndDateRange(String patientId, LocalDate startDate, LocalDate endDate);

    // === PLANLAMA YARDIMCILARI ===
    
    /**
     * Çakışan randevuları bul
     */
    List<Appointment> findConflictingAppointments(String doctorId, LocalDate date, 
                                                 LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Çakışan randevu var mı kontrol et
     */
    boolean hasConflictingAppointment(String doctorId, LocalDate date, 
                                     LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Yaklaşan randevuları getir
     */
    List<Appointment> findUpcomingAppointments(String userId, int days);
    
    /**
     * Bugünkü randevuları getir
     */
    List<Appointment> findTodayAppointments(String doctorId);

    // === HATIRLATMA VE BİLDİRİM ===
    
    /**
     * Hatırlatma gönderilecek randevuları bul
     */
    List<Appointment> findAppointmentsForReminder(LocalDateTime reminderTime);
    
    /**
     * Randevu hatırlatması gönder
     */
    void sendAppointmentReminder(String appointmentId);
    
    /**
     * Hatırlatma gönderildi olarak işaretle
     */
    void markReminderSent(String appointmentId);

    // === İSTATİSTİKLER ===
    
    /**
     * Duruma göre randevu sayısı
     */
    long countAppointmentsByStatus(AppointmentStatus status);
    
    /**
     * Doktorun randevu sayısı
     */
    long countAppointmentsByDoctor(String doctorId);
    
    /**
     * Hastanın randevu sayısı
     */
    long countAppointmentsByPatient(String patientId);
    
    /**
     * Tamamlanma oranını hesapla
     */
    double calculateCompletionRate(String doctorId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Gelmeme oranını hesapla
     */
    double calculateNoShowRate(String doctorId, LocalDate startDate, LocalDate endDate);
}