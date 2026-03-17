package com.healthvia.platform.doctor.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.doctor.entity.Doctor;

public interface DoctorService {

    // === BASIC CRUD OPERATIONS ===
    Doctor createDoctor(Doctor doctor);
    Doctor updateDoctor(String id, Doctor doctor);
    Optional<Doctor> findById(String id);
    void deleteDoctor(String id, String deletedBy);
    Page<Doctor> findAll(Pageable pageable);

    // === PROFILE MANAGEMENT ===
    Doctor updateProfile(String doctorId, String biography, String curriculum);
    Doctor updateWorkingHours(String doctorId, Set<String> workingDays, 
                             LocalTime startTime, LocalTime endTime);
    Doctor updateConsultationInfo(String doctorId, BigDecimal fee, Integer duration);
    Doctor addCertification(String doctorId, Doctor.Certification certification);
    Doctor updateVerificationStatus(String doctorId, Doctor.VerificationStatus status);

    // === SEARCH & FILTER ===
    Page<Doctor> searchDoctors(String searchTerm, Pageable pageable);
    List<Doctor> findBySpecialty(String specialty);
    List<Doctor> findByLocation(String province, String district);
    List<Doctor> findVerifiedDoctors();
    List<Doctor> findAvailableDoctors();
    Page<Doctor> findDoctorsWithFilters(String specialty, String province, 
                                       Double minRating, BigDecimal maxFee, Pageable pageable);

    // === APPOINTMENT MANAGEMENT ===
    Doctor updateAppointmentStatistics(String doctorId, int totalAppointments, 
                                      int completedAppointments, int cancelledAppointments);
    List<Doctor> findDoctorsWithHighCompletionRate(double minRate);
    Doctor setAcceptingPatients(String doctorId, boolean accepting);
    
    // === VALIDATION ===
    boolean isDiplomaNumberAvailable(String diplomaNumber);
    boolean isMedicalLicenseNumberAvailable(String medicalLicenseNumber);
    boolean canTreatCondition(String doctorId, String condition);
    
    // === ANALYTICS ===
    long countVerifiedDoctors();
    long countDoctorsBySpecialty(String specialty);
    List<Doctor> findTopRatedDoctors(int limit);
}
