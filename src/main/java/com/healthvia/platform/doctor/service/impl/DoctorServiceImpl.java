package com.healthvia.platform.doctor.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.doctor.entity.Doctor;
import com.healthvia.platform.doctor.repository.DoctorRepository;
import com.healthvia.platform.doctor.service.DoctorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    @Override
    public Doctor createDoctor(Doctor doctor) {
        validateDoctorForCreation(doctor);
        
        if (!isDiplomaNumberAvailable(doctor.getDiplomaNumber())) {
            throw new BusinessException(null, "Diploma number already exists");
        }
        
        if (!isMedicalLicenseNumberAvailable(doctor.getMedicalLicenseNumber())) {
            throw new BusinessException(null, "Medical license number already exists");
        }
        
        // Default values
        if (doctor.getVerificationStatus() == null) {
            doctor.setVerificationStatus(Doctor.VerificationStatus.PENDING);
        }
        if (doctor.getIsAcceptingNewPatients() == null) {
            doctor.setIsAcceptingNewPatients(true);
        }
        
        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor updateDoctor(String id, Doctor doctor) {
        Doctor existingDoctor = findByIdOrThrow(id);
        updateDoctorFields(existingDoctor, doctor);
        return doctorRepository.save(existingDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Doctor> findById(String id) {
        return doctorRepository.findById(id).filter(doctor -> !doctor.isDeleted());
    }

    @Override
    public void deleteDoctor(String id, String deletedBy) {
        Doctor doctor = findByIdOrThrow(id);
        doctor.markAsDeleted(deletedBy);
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Doctor> findAll(Pageable pageable) {
        return doctorRepository.findAll(pageable);
    }

    // === PROFILE MANAGEMENT ===

    @Override
    public Doctor updateProfile(String doctorId, String biography, String curriculum) {
        Doctor doctor = findByIdOrThrow(doctorId);
        doctor.setBiography(biography);
        doctor.setCurriculum(curriculum);
        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor updateWorkingHours(String doctorId, Set<String> workingDays, 
                                   LocalTime startTime, LocalTime endTime) {
        Doctor doctor = findByIdOrThrow(doctorId);
        doctor.setWorkingDays(workingDays);
        doctor.setWorkingHoursStart(startTime);
        doctor.setWorkingHoursEnd(endTime);
        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor updateConsultationInfo(String doctorId, BigDecimal fee, Integer duration) {
        Doctor doctor = findByIdOrThrow(doctorId);
        doctor.setConsultationFee(fee);
        doctor.setConsultationDurationMinutes(duration);
        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor addCertification(String doctorId, Doctor.Certification certification) {
        Doctor doctor = findByIdOrThrow(doctorId);
        List<Doctor.Certification> certifications = doctor.getCertifications();
        if (certifications != null) {
            certifications.add(certification);
        }
        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor updateVerificationStatus(String doctorId, Doctor.VerificationStatus status) {
        Doctor doctor = findByIdOrThrow(doctorId);
        doctor.setVerificationStatus(status);
        if (status == Doctor.VerificationStatus.VERIFIED) {
            doctor.setVerificationDate(LocalDate.now());
        }
        return doctorRepository.save(doctor);
    }

    // === SEARCH & FILTER ===

    @Override
    @Transactional(readOnly = true)
    public Page<Doctor> searchDoctors(String searchTerm, Pageable pageable) {
        return doctorRepository.searchDoctors(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findBySpecialty(String specialty) {
        return doctorRepository.findBySpecialtyContaining(specialty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findByLocation(String province, String district) {
        return doctorRepository.findByProvinceAndDistrictAndDeletedFalse(province, district);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findVerifiedDoctors() {
        return doctorRepository.findVerifiedDoctors();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findAvailableDoctors() {
        return doctorRepository.findAvailableDoctors();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Doctor> findDoctorsWithFilters(String specialty, String province, 
                                             Double minRating, BigDecimal maxFee, Pageable pageable) {
        return doctorRepository.findDoctorsWithFilters(specialty, province, minRating, maxFee, pageable);
    }

    // === APPOINTMENT MANAGEMENT ===

    @Override
    public Doctor updateAppointmentStatistics(String doctorId, int totalAppointments, 
                                             int completedAppointments, int cancelledAppointments) {
        Doctor doctor = findByIdOrThrow(doctorId);
        doctor.setTotalAppointments(totalAppointments);
        doctor.setCompletedAppointments(completedAppointments);
        doctor.setCancelledAppointments(cancelledAppointments);
        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findDoctorsWithHighCompletionRate(double minRate) {
        return doctorRepository.findDoctorsWithHighCompletionRate(minRate);
    }

    @Override
    public Doctor setAcceptingPatients(String doctorId, boolean isAccepting) {
        Doctor doctor = findByIdOrThrow(doctorId);
        doctor.setIsAcceptingNewPatients(isAccepting);
        return doctorRepository.save(doctor);
    }

    // === VALIDATION ===

    @Override
    @Transactional(readOnly = true)
    public boolean isDiplomaNumberAvailable(String diplomaNumber) {
        return !doctorRepository.existsByDiplomaNumberAndDeletedFalse(diplomaNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMedicalLicenseNumberAvailable(String medicalLicenseNumber) {
        return !doctorRepository.existsByMedicalLicenseNumberAndDeletedFalse(medicalLicenseNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canTreatCondition(String doctorId, String condition) {
        Doctor doctor = findByIdOrThrow(doctorId);
        return doctor.getTreatableConditions() != null && 
               doctor.getTreatableConditions().contains(condition);
    }

    // === ANALYTICS ===

    @Override
    @Transactional(readOnly = true)
    public long countVerifiedDoctors() {
        return doctorRepository.countVerifiedDoctors();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDoctorsBySpecialty(String specialty) {
        return doctorRepository.countByPrimarySpecialtyAndDeletedFalse(specialty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findTopRatedDoctors(int limit) {
        return doctorRepository.findAllOrderByAverageRatingDesc(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // === HELPER METHODS ===

    private Doctor findByIdOrThrow(String id) {
        return doctorRepository.findById(id)
            .filter(doctor -> !doctor.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private void validateDoctorForCreation(Doctor doctor) {
        if (doctor.getDiplomaNumber() == null || doctor.getDiplomaNumber().trim().isEmpty()) {
            throw new BusinessException(null, "Diploma number is required");
        }
        if (doctor.getMedicalLicenseNumber() == null || doctor.getMedicalLicenseNumber().trim().isEmpty()) {
            throw new BusinessException(null, "Medical license number is required");
        }
        if (doctor.getPrimarySpecialty() == null || doctor.getPrimarySpecialty().trim().isEmpty()) {
            throw new BusinessException(null, "Primary specialty is required");
        }
    }

    private void updateDoctorFields(Doctor existingDoctor, Doctor newDoctor) {
        // Professional info
        if (newDoctor.getBiography() != null) {
            existingDoctor.setBiography(newDoctor.getBiography());
        }
        if (newDoctor.getCurriculum() != null) {
            existingDoctor.setCurriculum(newDoctor.getCurriculum());
        }
        if (newDoctor.getPrimarySpecialty() != null) {
            existingDoctor.setPrimarySpecialty(newDoctor.getPrimarySpecialty());
        }
        
        // Working hours
        if (newDoctor.getWorkingDays() != null) {
            existingDoctor.setWorkingDays(newDoctor.getWorkingDays());
        }
        if (newDoctor.getWorkingHoursStart() != null) {
            existingDoctor.setWorkingHoursStart(newDoctor.getWorkingHoursStart());
        }
        if (newDoctor.getWorkingHoursEnd() != null) {
            existingDoctor.setWorkingHoursEnd(newDoctor.getWorkingHoursEnd());
        }
        
        // Consultation info
        if (newDoctor.getConsultationFee() != null) {
            existingDoctor.setConsultationFee(newDoctor.getConsultationFee());
        }
        if (newDoctor.getConsultationDurationMinutes() != null) {
            existingDoctor.setConsultationDurationMinutes(newDoctor.getConsultationDurationMinutes());
        }
        
        // Availability
        if (newDoctor.getIsAcceptingNewPatients() != null) {
            existingDoctor.setIsAcceptingNewPatients(newDoctor.getIsAcceptingNewPatients());
        }
        if (newDoctor.getIsAvailableForEmergencies() != null) {
            existingDoctor.setIsAvailableForEmergencies(newDoctor.getIsAvailableForEmergencies());
        }

        // Hospital & Clinic
        if (newDoctor.getCurrentHospital() != null) {
            existingDoctor.setCurrentHospital(newDoctor.getCurrentHospital());
        }
        if (newDoctor.getCurrentClinic() != null) {
            existingDoctor.setCurrentClinic(newDoctor.getCurrentClinic());
        }
    }

    @Override
    public Doctor syncFromPayload(String doctorId, String hospitalName, String primarySpecialty,
                                   String shortBio, Integer yearsOfExperience) {
        Doctor doctor = findByIdOrThrow(doctorId);

        if (hospitalName != null && !hospitalName.isBlank() && hospitalName.length() <= 200) {
            doctor.setCurrentHospital(hospitalName.trim());
        }
        if (primarySpecialty != null && !primarySpecialty.isBlank()) {
            doctor.setPrimarySpecialty(primarySpecialty.trim());
        }
        if (shortBio != null && !shortBio.isBlank() && shortBio.length() <= 2000) {
            doctor.setBiography(shortBio.trim());
        }
        if (yearsOfExperience != null && yearsOfExperience >= 0 && yearsOfExperience <= 70) {
            doctor.setYearsOfExperience(yearsOfExperience);
        }

        log.info("Synced doctor {} from Payload CMS", doctorId);
        return doctorRepository.save(doctor);
    }
}