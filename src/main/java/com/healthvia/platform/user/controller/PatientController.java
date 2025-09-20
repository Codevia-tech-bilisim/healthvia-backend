// user/controller/PatientController.java
package com.healthvia.platform.user.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.common.util.TcKimlikNoValidator;
import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.entity.User;
import com.healthvia.platform.user.service.PatientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // === PUBLIC ENDPOINTS ===
    
    @GetMapping("/public/count")
    public ApiResponse<Long> getPatientCount() {
        long count = patientService.countPatientsWithInsurance(); // Placeholder
        return ApiResponse.success(count);
    }

    // === PATIENT PROFILE MANAGEMENT ===
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> getMyProfile() {
        String patientId = SecurityUtils.getCurrentUserId();
        return patientService.findById(patientId)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Patient profile not found"));
    }
    
    @PatchMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyProfile(@RequestBody Patient patient) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updatePatient(patientId, patient);
        return ApiResponse.success(updatedPatient, "Profile updated successfully");
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or (#id == authentication.principal.id and hasRole('PATIENT'))")
    public ApiResponse<Patient> getPatientById(@PathVariable String id) {
        return patientService.findById(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Patient not found"));
    }

    // === HEALTH INFORMATION MANAGEMENT ===
    
    @PatchMapping("/me/health")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyHealthInfo(
            @RequestParam(required = false) String allergies,
            @RequestParam(required = false) String chronicDiseases,
            @RequestParam(required = false) String currentMedications,
            @RequestParam(required = false) String familyMedicalHistory) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateHealthInformation(
            patientId, allergies, chronicDiseases, currentMedications, familyMedicalHistory);
        return ApiResponse.success(updatedPatient, "Health information updated successfully");
    }
    
    @PatchMapping("/me/blood-type")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyBloodType(@RequestParam String bloodType) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateBloodType(patientId, bloodType);
        return ApiResponse.success(updatedPatient, "Blood type updated successfully");
    }
    
    @PatchMapping("/me/physical")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyPhysicalMeasurements(
            @RequestParam(required = false) Integer heightCm,
            @RequestParam(required = false) Double weightKg) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updatePhysicalMeasurements(patientId, heightCm, weightKg);
        return ApiResponse.success(updatedPatient, "Physical measurements updated successfully");
    }
    
    @GetMapping("/me/bmi")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Double> getMyBMI() {
        String patientId = SecurityUtils.getCurrentUserId();
        Double bmi = patientService.calculateBMI(patientId);
        return ApiResponse.success(bmi);
    }
    
    @GetMapping("/me/bmi-category")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<String> getMyBMICategory() {
        String patientId = SecurityUtils.getCurrentUserId();
        String category = patientService.getBMICategory(patientId);
        return ApiResponse.success(category, "Success");
    }

    // === INSURANCE MANAGEMENT ===
    
    @PatchMapping("/me/insurance")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyInsurance(
            @RequestParam String insuranceCompany,
            @RequestParam String policyNumber,
            @RequestParam LocalDate expiryDate) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateInsuranceInformation(
            patientId, insuranceCompany, policyNumber, expiryDate);
        return ApiResponse.success(updatedPatient, "Insurance information updated successfully");
    }
    
    @PatchMapping("/me/insurance/status")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyInsuranceStatus(@RequestParam boolean hasInsurance) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateInsuranceStatus(patientId, hasInsurance);
        return ApiResponse.success(updatedPatient, "Insurance status updated successfully");
    }

    // === EMERGENCY CONTACT MANAGEMENT ===
    
    @PatchMapping("/me/emergency-contact")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyEmergencyContact(
            @RequestParam String contactName,
            @RequestParam String contactPhone,
            @RequestParam String relationship) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateEmergencyContact(
            patientId, contactName, contactPhone, relationship);
        return ApiResponse.success(updatedPatient, "Emergency contact updated successfully");
    }

    // === LIFESTYLE MANAGEMENT ===
    
    @PatchMapping("/me/lifestyle")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyLifestyle(
            @RequestParam(required = false) Patient.SmokingStatus smokingStatus,
            @RequestParam(required = false) Patient.AlcoholConsumption alcoholConsumption,
            @RequestParam(required = false) Patient.ExerciseFrequency exerciseFrequency) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateLifestyleInformation(
            patientId, smokingStatus, alcoholConsumption, exerciseFrequency);
        return ApiResponse.success(updatedPatient, "Lifestyle information updated successfully");
    }
    
    @PatchMapping("/me/doctor-preference")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Patient> updateMyDoctorPreference(@RequestParam User.Gender preferredGender) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updatePreferredDoctorGender(patientId, preferredGender);
        return ApiResponse.success(updatedPatient, "Doctor preference updated successfully");
    }

    // === ADMIN ENDPOINTS ===
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<Page<Patient>> getAllPatients(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Patient> patients = patientService.findAll(pageable);
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<Page<Patient>> searchPatients(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Patient> patients = patientService.searchPatients(searchTerm, pageable);
        return ApiResponse.success(patients);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Patient> createPatient(@Valid @RequestBody Patient patient) {
        Patient createdPatient = patientService.createPatient(patient);
        return ApiResponse.success(createdPatient, "Patient created successfully");
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deletePatient(@PathVariable String id) {
        String deletedBy = SecurityUtils.getCurrentUserId();
        patientService.deletePatient(id, deletedBy);
        return ApiResponse.success("Patient deleted successfully");
    }

    // === SEARCH & FILTER ENDPOINTS ===
    
    @GetMapping("/by-blood-type/{bloodType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getPatientsByBloodType(@PathVariable String bloodType) {
        List<Patient> patients = patientService.findByBloodType(bloodType);
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/with-allergies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getPatientsWithAllergies() {
        List<Patient> patients = patientService.findPatientsWithAllergies();
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/with-chronic-diseases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getPatientsWithChronicDiseases() {
        List<Patient> patients = patientService.findPatientsWithChronicDiseases();
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/by-location")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getPatientsByLocation(
            @RequestParam String province,
            @RequestParam(required = false) String district) {
        List<Patient> patients = patientService.findByLocation(province, district);
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/by-age-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getPatientsByAgeRange(
            @RequestParam int minAge,
            @RequestParam int maxAge) {
        List<Patient> patients = patientService.findPatientsByAgeRange(minAge, maxAge);
        return ApiResponse.success(patients);
    }

    // === INSURANCE QUERIES ===
    
    @GetMapping("/with-insurance")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPatientsWithInsuranceCount() {
        long count = patientService.countPatientsWithInsurance();
        return ApiResponse.success(count);
    }
    
    @GetMapping("/insurance-expiring")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Patient>> getPatientsWithExpiringInsurance(
            @RequestParam(defaultValue = "30") int daysBeforeExpiry) {
        List<Patient> patients = patientService.findPatientsWithExpiringInsurance(daysBeforeExpiry);
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/insurance-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Patient>> getPatientsWithExpiredInsurance() {
        List<Patient> patients = patientService.findPatientsWithExpiredInsurance();
        return ApiResponse.success(patients);
    }

    // === EMERGENCY CONTACT QUERIES ===
    
    @GetMapping("/without-emergency-contact")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Patient>> getPatientsWithoutEmergencyContact() {
        List<Patient> patients = patientService.findPatientsWithoutEmergencyContact();
        return ApiResponse.success(patients);
    }

    // === HEALTH ANALYTICS ===
    
    @GetMapping("/obese")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getObesePatients() {
        List<Patient> patients = patientService.findObesePatients();
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/high-risk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<Patient>> getHighRiskPatients() {
        List<Patient> patients = patientService.findHighRiskPatients();
        return ApiResponse.success(patients);
    }

    // === APPOINTMENT STATISTICS ===
    
    @GetMapping("/without-appointments")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Patient>> getPatientsWithoutAppointments() {
        List<Patient> patients = patientService.findPatientsWithoutAppointments();
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Patient>> getInactivePatients(
            @RequestParam(defaultValue = "90") int daysSinceLastAppointment) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysSinceLastAppointment);
        List<Patient> patients = patientService.findInactivePatients(cutoffDate);
        return ApiResponse.success(patients);
    }
    
    @GetMapping("/top-by-appointments")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Patient>> getTopPatientsByAppointments(
            @RequestParam(defaultValue = "10") int limit) {
        List<Patient> patients = patientService.findTopPatientsByAppointments(limit);
        return ApiResponse.success(patients);
    }

    // === VALIDATION ENDPOINTS ===
    
    @GetMapping("/check-tc-kimlik")
    public ApiResponse<Boolean> checkTcKimlikAvailability(@RequestParam String tcKimlikNo) {
        boolean available = patientService.isTcKimlikNoAvailable(tcKimlikNo);
        return ApiResponse.success(available);
    }
    
    @GetMapping("/check-passport")
    public ApiResponse<Boolean> checkPassportAvailability(@RequestParam String passportNo) {
        boolean available = patientService.isPassportNoAvailable(passportNo);
        return ApiResponse.success(available);
    }
    
    @GetMapping("/validate-tc-kimlik")
    public ApiResponse<Boolean> validateTcKimlik(@RequestParam String tcKimlikNo) {
        boolean valid = TcKimlikNoValidator.isValid(tcKimlikNo);
        String message = valid ? "TC Kimlik No geçerli" : "TC Kimlik No geçersiz";
        return ApiResponse.success(valid, message);
    }

    @GetMapping("/me/masked-tc")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<String> getMyMaskedTc() {
        String patientId = SecurityUtils.getCurrentUserId();
        return patientService.findById(patientId)
            .map(patient -> TcKimlikNoValidator.mask(patient.getTcKimlikNo()))
            .map(masked -> ApiResponse.success(masked, "TC Kimlik No maskelendi"))
            .orElse(ApiResponse.error("Patient not found"));
    }
    
    @GetMapping("/validate-blood-type")
    public ApiResponse<Boolean> validateBloodType(@RequestParam String bloodType) {
        boolean valid = patientService.isValidBloodType(bloodType);
        return ApiResponse.success(valid);
    }

    // === STATISTICS ===
    
    @GetMapping("/statistics/health-issues-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPatientsWithHealthIssuesCount() {
        long count = patientService.countPatientsWithHealthIssues();
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/bmi-data-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPatientsWithBMIDataCount() {
        long count = patientService.countPatientsWithBMIData();
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/by-smoking")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPatientsBySmoking(@RequestParam Patient.SmokingStatus status) {
        long count = patientService.countPatientsBySmokingStatus(status);
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/by-alcohol")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPatientsByAlcohol(@RequestParam Patient.AlcoholConsumption consumption) {
        long count = patientService.countPatientsByAlcoholConsumption(consumption);
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/by-exercise")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getPatientsByExercise(@RequestParam Patient.ExerciseFrequency frequency) {
        long count = patientService.countPatientsByExerciseFrequency(frequency);
        return ApiResponse.success(count);
    }
}