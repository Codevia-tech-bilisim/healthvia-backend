package com.healthvia.platform.user.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.common.util.TcKimlikNoValidator;
import com.healthvia.platform.user.dto.PatientDto;
import com.healthvia.platform.user.dto.request.PatientCreateRequest;
import com.healthvia.platform.user.dto.request.PatientUpdateRequest;
import com.healthvia.platform.user.dto.response.PatientResponseDto;
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
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // === PUBLIC ENDPOINTS ===
    
    @GetMapping("/public/count")
    public ApiResponse<Long> getPatientCount() {
        long count = patientService.countPatientsWithInsurance();
        return ApiResponse.success(count);
    }

    // === PATIENT PROFILE MANAGEMENT ===
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> getMyProfile() {
        String patientId = SecurityUtils.getCurrentUserId();
        return patientService.findById(patientId)
            .map(PatientDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Patient profile not found"));
    }
    
    @GetMapping("/me/public")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientResponseDto> getMyPublicProfile() {
        String patientId = SecurityUtils.getCurrentUserId();
        return patientService.findById(patientId)
            .map(PatientResponseDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Patient profile not found"));
    }
    
    @PatchMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyProfile(@Valid @RequestBody PatientUpdateRequest request) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient patient = patientService.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
        
        // Update only provided fields
        if (request.getFirstName() != null) patient.setFirstName(request.getFirstName());
        if (request.getLastName() != null) patient.setLastName(request.getLastName());
        if (request.getPhone() != null) patient.setPhone(request.getPhone());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        if (request.getProvince() != null) patient.setProvince(request.getProvince());
        if (request.getDistrict() != null) patient.setDistrict(request.getDistrict());
        if (request.getPostalCode() != null) patient.setPostalCode(request.getPostalCode());
        
        Patient updatedPatient = patientService.updatePatient(patientId, patient);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Profile updated successfully");
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or (#id == authentication.principal.id and hasRole('PATIENT'))")
    public ApiResponse<PatientResponseDto> getPatientById(@PathVariable String id) {
        return patientService.findById(id)
            .map(PatientResponseDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Patient not found"));
    }

    // === HEALTH INFORMATION MANAGEMENT ===
    
    @PatchMapping("/me/health")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyHealthInfo(
            @RequestParam(required = false) String allergies,
            @RequestParam(required = false) String chronicDiseases,
            @RequestParam(required = false) String currentMedications,
            @RequestParam(required = false) String familyMedicalHistory) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateHealthInformation(
            patientId, allergies, chronicDiseases, currentMedications, familyMedicalHistory);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Health information updated successfully");
    }
    
    @PatchMapping("/me/blood-type")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyBloodType(@RequestParam String bloodType) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateBloodType(patientId, bloodType);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Blood type updated successfully");
    }
    
    @PatchMapping("/me/physical")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyPhysicalMeasurements(
            @RequestParam(required = false) Integer heightCm,
            @RequestParam(required = false) Double weightKg) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updatePhysicalMeasurements(patientId, heightCm, weightKg);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Physical measurements updated successfully");
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
        return ApiResponse.success(category, "BMI category retrieved successfully");
    }

    // === INSURANCE MANAGEMENT ===
    
    @PatchMapping("/me/insurance")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyInsurance(
            @RequestParam String insuranceCompany,
            @RequestParam String policyNumber,
            @RequestParam LocalDate expiryDate) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateInsuranceInformation(
            patientId, insuranceCompany, policyNumber, expiryDate);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Insurance information updated successfully");
    }
    
    @PatchMapping("/me/insurance/status")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyInsuranceStatus(@RequestParam boolean hasInsurance) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateInsuranceStatus(patientId, hasInsurance);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Insurance status updated successfully");
    }

    // === EMERGENCY CONTACT MANAGEMENT ===
    
    @PatchMapping("/me/emergency-contact")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyEmergencyContact(
            @RequestParam String contactName,
            @RequestParam String contactPhone,
            @RequestParam String relationship) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateEmergencyContact(
            patientId, contactName, contactPhone, relationship);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Emergency contact updated successfully");
    }

    // === LIFESTYLE MANAGEMENT ===
    
    @PatchMapping("/me/lifestyle")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyLifestyle(
            @RequestParam(required = false) Patient.SmokingStatus smokingStatus,
            @RequestParam(required = false) Patient.AlcoholConsumption alcoholConsumption,
            @RequestParam(required = false) Patient.ExerciseFrequency exerciseFrequency) {
        
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updateLifestyleInformation(
            patientId, smokingStatus, alcoholConsumption, exerciseFrequency);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Lifestyle information updated successfully");
    }
    
    @PatchMapping("/me/doctor-preference")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<PatientDto> updateMyDoctorPreference(@RequestParam User.Gender preferredGender) {
        String patientId = SecurityUtils.getCurrentUserId();
        Patient updatedPatient = patientService.updatePreferredDoctorGender(patientId, preferredGender);
        return ApiResponse.success(PatientDto.fromEntity(updatedPatient), "Doctor preference updated successfully");
    }

    // === ADMIN ENDPOINTS ===
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<Page<PatientDto>> getAllPatients(@PageableDefault(size = 20) Pageable pageable) {
        Page<Patient> patients = patientService.findAll(pageable);
        Page<PatientDto> patientDtos = patients.map(PatientDto::fromEntity);
        return ApiResponse.success(patientDtos);
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<Page<PatientDto>> searchPatients(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Patient> patients = patientService.searchPatients(searchTerm, pageable);
        Page<PatientDto> patientDtos = patients.map(PatientDto::fromEntity);
        return ApiResponse.success(patientDtos);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PatientDto> createPatient(@Valid @RequestBody PatientCreateRequest request) {
        // Convert request to entity
        Patient patient = Patient.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .gender(request.getGender())
            .birthDate(request.getBirthDate())
            .province(request.getProvince())
            .district(request.getDistrict())
            .tcKimlikNo(request.getTcKimlikNo())
            .passportNo(request.getPassportNo())
            .birthPlace(request.getBirthPlace())
            .address(request.getAddress())
            .postalCode(request.getPostalCode())
            .build();
        
        Patient createdPatient = patientService.createPatient(patient);
        return ApiResponse.success(PatientDto.fromEntity(createdPatient), "Patient created successfully");
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
    public ApiResponse<List<PatientResponseDto>> getPatientsByBloodType(@PathVariable String bloodType) {
        List<Patient> patients = patientService.findByBloodType(bloodType);
        List<PatientResponseDto> dtos = patients.stream()
            .map(PatientResponseDto::fromEntity)
            .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }
    
    @GetMapping("/with-allergies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<PatientResponseDto>> getPatientsWithAllergies() {
        List<Patient> patients = patientService.findPatientsWithAllergies();
        List<PatientResponseDto> dtos = patients.stream()
            .map(PatientResponseDto::fromEntity)
            .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }
    
    @GetMapping("/with-chronic-diseases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<List<PatientResponseDto>> getPatientsWithChronicDiseases() {
        List<Patient> patients = patientService.findPatientsWithChronicDiseases();
        List<PatientResponseDto> dtos = patients.stream()
            .map(PatientResponseDto::fromEntity)
            .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }
    
    // Diğer endpoint'ler benzer şekilde güncellenir...
    
    // === VALIDATION ENDPOINTS (bunlar değişmeden kalabilir) ===
    
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
