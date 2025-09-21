// user/service/impl/PatientServiceImpl.java
package com.healthvia.platform.user.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.constants.ErrorCodes;
import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.common.util.TcKimlikNoValidator;
import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.entity.User;
import com.healthvia.platform.user.repository.PatientRepository;
import com.healthvia.platform.user.service.PatientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    // === BASIC CRUD OPERATIONS ===
    
    @Override
    public Patient createPatient(Patient patient) {
        // Tüm validasyonları çalıştır
        validatePatientForCreation(patient);
        
        // TC Kimlik No benzersizlik kontrolü
        if (patient.getTcKimlikNo() != null 
                && !isTcKimlikNoAvailable(patient.getTcKimlikNo())) {
            throw new BusinessException(
                ErrorCodes.VALIDATION_ERROR, 
                "TC Kimlik No zaten kullanımda"
            );
        }
        
        // Pasaport No benzersizlik kontrolü
        if (patient.getPassportNo() != null 
                && !isPassportNoAvailable(patient.getPassportNo())) {
            throw new BusinessException(
                ErrorCodes.VALIDATION_ERROR, 
                "Pasaport No zaten kullanımda"
            );
        }
        
        // Hasta kaydını veritabanına yaz
        return patientRepository.save(patient);
    }
    
    private void validatePatientForCreation(Patient patient) {
        // TC Kimlik No algoritma kontrolü
        if (patient.getTcKimlikNo() != null && !patient.getTcKimlikNo().isEmpty()) {
            if (!TcKimlikNoValidator.isValid(patient.getTcKimlikNo())) {
                throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR, 
                    "Geçersiz TC Kimlik Numarası"
                );
            }
            // TC'yi formatla
            patient.setTcKimlikNo(
                TcKimlikNoValidator.format(patient.getTcKimlikNo())
            );
        }
        
        // Ek profil validasyonu
        if (!validatePatientProfile(patient)) {
            throw new BusinessException(
                ErrorCodes.VALIDATION_ERROR, 
                "Invalid patient profile data"
            );
        }
    }
    @Override
    public Patient updatePatient(String id, Patient patient) {
        Patient existingPatient = findByIdOrThrow(id);
        updatePatientFields(existingPatient, patient);
        return patientRepository.save(existingPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findById(String id) {
        return patientRepository.findById(id).filter(patient -> !patient.isDeleted());
    }

    @Override
    public void deletePatient(String id, String deletedBy) {
        Patient patient = findByIdOrThrow(id);
        patient.markAsDeleted(deletedBy);
        patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }
    @Override
    public String getMaskedTcKimlikNo(String patientId) {
        Patient patient = findByIdOrThrow(patientId);
        return TcKimlikNoValidator.mask(patient.getTcKimlikNo());
    }

    // === IDENTITY BASED OPERATIONS ===
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findByTcKimlikNo(String tcKimlikNo) {
        return patientRepository.findByTcKimlikNoAndDeletedFalse(tcKimlikNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findByPassportNo(String passportNo) {
        return patientRepository.findByPassportNoAndDeletedFalse(passportNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findByIdentityNumber(String identityNumber) {
        return patientRepository.findByTcKimlikNoOrPassportNo(identityNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTcKimlikNoAvailable(String tcKimlikNo) {
        return !patientRepository.existsByTcKimlikNoAndDeletedFalse(tcKimlikNo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPassportNoAvailable(String passportNo) {
        return !patientRepository.existsByPassportNoAndDeletedFalse(passportNo);
    }

    // === HEALTH INFORMATION MANAGEMENT ===
    
    @Override
    public Patient updateHealthInformation(String patientId, String allergies, String chronicDiseases, 
                                         String currentMedications, String familyMedicalHistory) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setAllergies(allergies);
        patient.setChronicDiseases(chronicDiseases);
        patient.setCurrentMedications(currentMedications);
        patient.setFamilyMedicalHistory(familyMedicalHistory);
        return patientRepository.save(patient);
    }

    @Override
    public Patient updateBloodType(String patientId, String bloodType) {
        if (!isValidBloodType(bloodType)) {
            throw new BusinessException(null, "Geçersiz kan grubu");
        }
        Patient patient = findByIdOrThrow(patientId);
        patient.setBloodType(bloodType);
        return patientRepository.save(patient);
    }

    @Override
    public Patient updatePhysicalMeasurements(String patientId, Integer heightCm, Double weightKg) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setHeightCm(heightCm);
        patient.setWeightKg(weightKg);
        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateBMI(String patientId) {
        Patient patient = findByIdOrThrow(patientId);
        return patient.getBMI();
    }

    @Override
    @Transactional(readOnly = true)
    public String getBMICategory(String patientId) {
        Patient patient = findByIdOrThrow(patientId);
        return patient.getBMICategory();
    }

    // === INSURANCE MANAGEMENT ===
    
    @Override
    public Patient updateInsuranceInformation(String patientId, String insuranceCompany, 
                                            String policyNumber, LocalDate expiryDate) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setInsuranceCompany(insuranceCompany);
        patient.setInsurancePolicyNumber(policyNumber);
        patient.setInsuranceExpiryDate(expiryDate);
        patient.setHasInsurance(true);
        return patientRepository.save(patient);
    }

    @Override
    public Patient updateInsuranceStatus(String patientId, boolean hasInsurance) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setHasInsurance(hasInsurance);
        if (!hasInsurance) {
            patient.setInsuranceCompany(null);
            patient.setInsurancePolicyNumber(null);
            patient.setInsuranceExpiryDate(null);
        }
        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithExpiringInsurance(int daysBeforeExpiry) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(daysBeforeExpiry);
        return patientRepository.findPatientsWithExpiringInsurance(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithExpiredInsurance() {
        return patientRepository.findPatientsWithExpiredInsurance(LocalDate.now());
    }

    // === EMERGENCY CONTACT MANAGEMENT ===
    
    @Override
    public Patient updateEmergencyContact(String patientId, String contactName, 
                                        String contactPhone, String relationship) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setEmergencyContactName(contactName);
        patient.setEmergencyContactPhone(contactPhone);
        patient.setEmergencyContactRelationship(relationship);
        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithEmergencyContact() {
        return patientRepository.findPatientsWithEmergencyContact();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithoutEmergencyContact() {
        return patientRepository.findPatientsWithoutEmergencyContact();
    }

    // === LIFESTYLE MANAGEMENT ===
    
    @Override
    public Patient updateLifestyleInformation(String patientId, Patient.SmokingStatus smokingStatus,
                                            Patient.AlcoholConsumption alcoholConsumption,
                                            Patient.ExerciseFrequency exerciseFrequency) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setSmokingStatus(smokingStatus);
        patient.setAlcoholConsumption(alcoholConsumption);
        patient.setExerciseFrequency(exerciseFrequency);
        return patientRepository.save(patient);
    }

    @Override
    public Patient updatePreferredDoctorGender(String patientId, User.Gender preferredGender) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setPreferredDoctorGender(preferredGender);
        return patientRepository.save(patient);
    }

    // === SEARCH & FILTER OPERATIONS ===
    
    @Override
    @Transactional(readOnly = true)
    public Page<Patient> searchPatients(String searchTerm, Pageable pageable) {
        return patientRepository.searchPatients(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findByBloodType(String bloodType) {
        return patientRepository.findByBloodTypeAndDeletedFalse(bloodType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithAllergies() {
        return patientRepository.findPatientsWithAllergies();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findByAllergyType(String allergyType) {
        return patientRepository.findByAllergiesContaining(allergyType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithChronicDiseases() {
        return patientRepository.findPatientsWithChronicDiseases();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findByChronicDisease(String disease) {
        return patientRepository.findByChronicDiseasesContaining(disease);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> searchByHealthConditions(String condition) {
        return patientRepository.searchByHealthConditions(condition);
    }

    // === LOCATION BASED OPERATIONS ===
    
    @Override
    @Transactional(readOnly = true)
    public List<Patient> findByLocation(String province, String district) {
        return patientRepository.findByProvinceAndDistrictAndDeletedFalse(province, district);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findByPostalCode(String postalCode) {
        return patientRepository.findByPostalCodeAndDeletedFalse(postalCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findByBirthPlace(String birthPlace) {
        return patientRepository.findByBirthPlaceContainingIgnoreCaseAndDeletedFalse(birthPlace);
    }

    // === AGE & DEMOGRAPHICS ===
    
    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsByAgeRange(int minAge, int maxAge) {
        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge + 1);
        return patientRepository.findPatientsByAgeRange(minBirthDate, maxBirthDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsByAgeGroup(int minAge, int maxAge) {
        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge + 1);
        return patientRepository.countPatientsByAgeGroup(minBirthDate, maxBirthDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsByGender(User.Gender gender) {
        // Bu method'u implement etmek için UserRepository'ye eklenmeli
        return 0; // Şimdilik placeholder
    }

    // === APPOINTMENT STATISTICS ===
    
    @Override
    public Patient updateAppointmentStatistics(String patientId, int totalAppointments, 
                                             int completedAppointments, int cancelledAppointments) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setTotalAppointments(totalAppointments);
        patient.setCompletedAppointments(completedAppointments);
        patient.setCancelledAppointments(cancelledAppointments);
        return patientRepository.save(patient);
    }

    @Override
    public Patient updateLastAppointmentDate(String patientId, LocalDate lastAppointmentDate) {
        Patient patient = findByIdOrThrow(patientId);
        patient.setLastAppointmentDate(lastAppointmentDate);
        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateAppointmentCompletionRate(String patientId) {
        Patient patient = findByIdOrThrow(patientId);
        return patient.getAppointmentCompletionRate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithoutAppointments() {
        return patientRepository.findPatientsWithoutAppointments();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findInactivePatients(LocalDate lastAppointmentBefore) {
        return patientRepository.findInactivePatientsSince(lastAppointmentBefore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findTopPatientsByAppointments(int limit) {
        return patientRepository.findTopPatientsByAppointmentCount(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // === ANALYTICS & REPORTING ===
    
    @Override
    @Transactional(readOnly = true)
    public long countPatientsWithInsurance() {
        return patientRepository.countByHasInsuranceTrueAndDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsWithHealthIssues() {
        return patientRepository.countPatientsWithHealthIssues();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsByLocation(String province, String district) {
        return patientRepository.countByProvinceAndDistrictAndDeletedFalse(province, district);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsWithBMIData() {
        return patientRepository.findPatientsWithBMIData().size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsBySmokingStatus(Patient.SmokingStatus status) {
        return patientRepository.findBySmokingStatusAndDeletedFalse(status).size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsByAlcoholConsumption(Patient.AlcoholConsumption consumption) {
        return patientRepository.findByAlcoholConsumptionAndDeletedFalse(consumption).size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPatientsByExerciseFrequency(Patient.ExerciseFrequency frequency) {
        return patientRepository.findByExerciseFrequencyAndDeletedFalse(frequency).size();
    }

    // === VALIDATION METHODS ===
    
    @Override
    public boolean validatePatientProfile(Patient patient) {
        return patient.hasValidIdentity() && 
               patient.getFirstName() != null && 
               patient.getLastName() != null &&
               patient.getEmail() != null;
    }

    @Override
    public boolean validateHealthInformation(String allergies, String chronicDiseases) {
        // Basit validasyon - gerçek projede daha detaylı olabilir
        return true;
    }

    @Override
    public boolean validateInsuranceInformation(String insuranceCompany, String policyNumber) {
        return insuranceCompany != null && !insuranceCompany.trim().isEmpty() &&
               policyNumber != null && !policyNumber.trim().isEmpty();
    }


    @Override
    public boolean isValidBloodType(String bloodType) {
        if (bloodType == null) return false;
        return bloodType.matches("^(A|B|AB|0)[+-]$");
    }

    // === BULK OPERATIONS - Basit implementasyon ===
    
    @Override
    public List<Patient> createPatients(List<Patient> patients) {
        return patientRepository.saveAll(patients);
    }

    @Override
    public void updatePatientsHealthInfo(List<String> patientIds, String healthInfo) {
        // Toplu güncelleme - gerçek projede daha optimize edilebilir
        patientIds.forEach(id -> {
            try {
                Patient patient = findByIdOrThrow(id);
                patient.setAllergies(healthInfo);
                patientRepository.save(patient);
            } catch (Exception e) {
                log.warn("Failed to update health info for patient {}: {}", id, e.getMessage());
            }
        });
    }

    @Override
    public void updatePatientsInsuranceStatus(List<String> patientIds, boolean hasInsurance) {
        patientIds.forEach(id -> {
            try {
                updateInsuranceStatus(id, hasInsurance);
            } catch (Exception e) {
                log.warn("Failed to update insurance status for patient {}: {}", id, e.getMessage());
            }
        });
    }

    // === Placeholder implementations - İleride gerçek implement edilecek ===
    
    @Override
    public List<Patient> findPatientsRequiringHealthAlerts() {
        // Sağlık uyarısı gerekenleri bul
        return List.of();
    }

    @Override
    public List<Patient> findPatientsForCheckupReminder(int monthsSinceLastAppointment) {
        LocalDate cutoffDate = LocalDate.now().minusMonths(monthsSinceLastAppointment);
        return findInactivePatients(cutoffDate);
    }

    @Override
    public List<Patient> findObesePatients() {
        return patientRepository.findPatientsWithBMIData().stream()
            .filter(patient -> {
                Double bmi = patient.getBMI();
                return bmi != null && bmi > 30.0;
            })
            .toList();
    }

    @Override
    public List<Patient> findHighRiskPatients() {
        // Risk grubu hastalar - yaş, kronik hastalık vs.
        return findPatientsWithChronicDiseases();
    }

    // === Placeholder methods - Gelecekte implement edilecek ===
    
    @Override
    public Patient anonymizePatientData(String patientId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String exportPatientData(String patientId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void permanentlyDeletePatientData(String patientId, String reason) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Patient importPatientFromExternalSystem(String externalPatientId, String systemName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean isValidTcKimlikNo(String tcKimlikNo) {
        return TcKimlikNoValidator.isValid(tcKimlikNo);
    }

    @Override
    public void syncPatientToExternalSystem(String patientId, String systemName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Patient> findPotentialDuplicates(String tcKimlikNo, String firstName, String lastName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // === HELPER METHODS ===
    
    private Patient findByIdOrThrow(String id) {
        return patientRepository.findById(id)
            .filter(patient -> !patient.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }
    

    
    private void updatePatientFields(Patient existingPatient, Patient newPatient) {
        // Sağlık bilgileri
        if (newPatient.getAllergies() != null) {
            existingPatient.setAllergies(newPatient.getAllergies());
        }
        if (newPatient.getChronicDiseases() != null) {
            existingPatient.setChronicDiseases(newPatient.getChronicDiseases());
        }
        if (newPatient.getCurrentMedications() != null) {
            existingPatient.setCurrentMedications(newPatient.getCurrentMedications());
        }
        if (newPatient.getBloodType() != null) {
            existingPatient.setBloodType(newPatient.getBloodType());
        }
        
        // Fiziksel ölçümler
        if (newPatient.getHeightCm() != null) {
            existingPatient.setHeightCm(newPatient.getHeightCm());
        }
        if (newPatient.getWeightKg() != null) {
            existingPatient.setWeightKg(newPatient.getWeightKg());
        }
        
        // Sigorta bilgileri
        if (newPatient.getInsuranceCompany() != null) {
            existingPatient.setInsuranceCompany(newPatient.getInsuranceCompany());
        }
        if (newPatient.getInsurancePolicyNumber() != null) {
            existingPatient.setInsurancePolicyNumber(newPatient.getInsurancePolicyNumber());
        }
        
        // Lifestyle
        if (newPatient.getSmokingStatus() != null) {
            existingPatient.setSmokingStatus(newPatient.getSmokingStatus());
        }
        if (newPatient.getAlcoholConsumption() != null) {
            existingPatient.setAlcoholConsumption(newPatient.getAlcoholConsumption());
        }
        if (newPatient.getExerciseFrequency() != null) {
            existingPatient.setExerciseFrequency(newPatient.getExerciseFrequency());
        }
    }
}