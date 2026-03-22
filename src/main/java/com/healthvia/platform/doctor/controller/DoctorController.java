package com.healthvia.platform.doctor.controller;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.doctor.dto.DoctorDto;
import com.healthvia.platform.doctor.entity.Doctor;
import com.healthvia.platform.doctor.service.DoctorService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @Value("${payload.sync.api-key}")
    private String payloadSyncApiKey;

    // === PUBLIC ENDPOINTS ===
    
    @GetMapping("/public/search")
    public ApiResponse<Page<DoctorDto>> searchPublicDoctors(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) BigDecimal maxFee,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Doctor> doctors = doctorService.findDoctorsWithFilters(
            specialty, province, minRating, maxFee, pageable);
        
        Page<DoctorDto> doctorDtos = doctors.map(DoctorDto::fromEntityBasic);
        return ApiResponse.success(doctorDtos);
    }
    
    @GetMapping("/public/verified")
    public ApiResponse<List<DoctorDto>> getVerifiedDoctors() {
        List<Doctor> doctors = doctorService.findVerifiedDoctors();
        List<DoctorDto> doctorDtos = doctors.stream()
            .map(DoctorDto::fromEntityBasic)
            .toList();
        return ApiResponse.success(doctorDtos);
    }
    
    @GetMapping("/public/available")
    public ApiResponse<List<DoctorDto>> getAvailableDoctors() {
        List<Doctor> doctors = doctorService.findAvailableDoctors();
        List<DoctorDto> doctorDtos = doctors.stream()
            .map(DoctorDto::fromEntityBasic)
            .toList();
        return ApiResponse.success(doctorDtos);
    }
    
    @GetMapping("/public/{id}")
    public ApiResponse<DoctorDto> getPublicDoctorProfile(@PathVariable String id) {
        return doctorService.findById(id)
            .map(DoctorDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Doctor not found"));
    }

    // === DOCTOR PROFILE MANAGEMENT ===
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> getMyProfile() {
        String doctorId = SecurityUtils.getCurrentUserId();
        return doctorService.findById(doctorId)
            .map(DoctorDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Doctor profile not found"));
    }
    
    @PatchMapping("/me/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> updateMyProfile(
            @RequestParam(required = false) String biography,
            @RequestParam(required = false) String curriculum) {
        String doctorId = SecurityUtils.getCurrentUserId();
        Doctor updatedDoctor = doctorService.updateProfile(doctorId, biography, curriculum);
        return ApiResponse.success(DoctorDto.fromEntity(updatedDoctor), "Profile updated successfully");
    }
    
    @PatchMapping("/me/working-hours")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> updateMyWorkingHours(
            @RequestParam Set<String> workingDays,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        String doctorId = SecurityUtils.getCurrentUserId();
        Doctor updatedDoctor = doctorService.updateWorkingHours(doctorId, workingDays, startTime, endTime);
        return ApiResponse.success(DoctorDto.fromEntity(updatedDoctor), "Working hours updated successfully");
    }
    
    @PatchMapping("/me/consultation")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> updateMyConsultationInfo(
            @RequestParam BigDecimal fee,
            @RequestParam Integer duration) {
        String doctorId = SecurityUtils.getCurrentUserId();
        Doctor updatedDoctor = doctorService.updateConsultationInfo(doctorId, fee, duration);
        return ApiResponse.success(DoctorDto.fromEntity(updatedDoctor), "Consultation info updated successfully");
    }
    
    @PostMapping("/me/certifications")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> addMyCertification(@RequestBody Doctor.Certification certification) {
        String doctorId = SecurityUtils.getCurrentUserId();
        Doctor updatedDoctor = doctorService.addCertification(doctorId, certification);
        return ApiResponse.success(DoctorDto.fromEntity(updatedDoctor), "Certification added successfully");
    }
    @PatchMapping("/me/accepting-patients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> setAcceptingPatients(
            @RequestParam boolean accepting) {
        String doctorId = SecurityUtils.getCurrentUserId();
        Doctor updated = doctorService.setAcceptingPatients(doctorId, accepting);
        return ApiResponse.success(DoctorDto.fromEntity(updated), "Güncellendi");
    }

    // === PAYLOAD CMS SYNC ENDPOINT ===

    @PatchMapping("/{id}/sync")
    @Operation(summary = "Sync doctor from Payload CMS", description = "Updates doctor fields from Payload CMS admin panel. Requires X-Payload-API-Key header.")
    public ApiResponse<DoctorDto> syncFromPayload(
            @PathVariable String id,
            @RequestHeader(value = "X-Payload-API-Key", required = false) String apiKey,
            @RequestParam(required = false) String hospitalName,
            @RequestParam(required = false) String primarySpecialty,
            @RequestParam(required = false) String shortBio,
            @RequestParam(required = false) Integer yearsOfExperience,
            @RequestParam(required = false) BigDecimal consultationFee,
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) Integer reviewCount,
            @RequestParam(required = false) String languages) {

        // Validate API key — only Payload CMS should call this endpoint
        if (apiKey == null || !apiKey.equals(payloadSyncApiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or missing API key");
        }

        Doctor updated = doctorService.syncFromPayload(id, hospitalName, primarySpecialty, shortBio,
                yearsOfExperience, consultationFee, rating, reviewCount, languages);
        return ApiResponse.success(DoctorDto.fromEntity(updated), "Doctor synced from Payload CMS");
    }

    // === ADMIN ENDPOINTS ===

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<DoctorDto>> getAllDoctors(@PageableDefault(size = 20) Pageable pageable) {
        Page<Doctor> doctors = doctorService.findAll(pageable);
        Page<DoctorDto> doctorDtos = doctors.map(DoctorDto::fromEntity);
        return ApiResponse.success(doctorDtos);
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<DoctorDto>> searchDoctors(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Doctor> doctors = doctorService.searchDoctors(searchTerm, pageable);
        Page<DoctorDto> doctorDtos = doctors.map(DoctorDto::fromEntity);
        return ApiResponse.success(doctorDtos);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorDto> createDoctor(@Valid @RequestBody Doctor doctor) {
        Doctor createdDoctor = doctorService.createDoctor(doctor);
        return ApiResponse.success(DoctorDto.fromEntity(createdDoctor), "Doctor created successfully");
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ApiResponse<DoctorDto> getDoctorById(@PathVariable String id) {
        return doctorService.findById(id)
            .map(DoctorDto::fromEntity)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Doctor not found"));
    }
    
    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorDto> updateVerificationStatus(
            @PathVariable String id,
            @RequestParam Doctor.VerificationStatus status) {
        Doctor updatedDoctor = doctorService.updateVerificationStatus(id, status);
        return ApiResponse.success(DoctorDto.fromEntity(updatedDoctor), "Verification status updated");
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDoctor(@PathVariable String id) {
        String deletedBy = SecurityUtils.getCurrentUserId();
        doctorService.deleteDoctor(id, deletedBy);
        return ApiResponse.success("Doctor deleted successfully");
    }

    // === SEARCH & FILTER ENDPOINTS ===
    
    @GetMapping("/by-specialty/{specialty}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    public ApiResponse<List<DoctorDto>> getDoctorsBySpecialty(@PathVariable String specialty) {
        List<Doctor> doctors = doctorService.findBySpecialty(specialty);
        List<DoctorDto> doctorDtos = doctors.stream()
            .map(DoctorDto::fromEntityBasic)
            .toList();
        return ApiResponse.success(doctorDtos);
    }
    
    @GetMapping("/by-location")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    public ApiResponse<List<DoctorDto>> getDoctorsByLocation(
            @RequestParam String province,
            @RequestParam(required = false) String district) {
        List<Doctor> doctors = doctorService.findByLocation(province, district);
        List<DoctorDto> doctorDtos = doctors.stream()
            .map(DoctorDto::fromEntityBasic)
            .toList();
        return ApiResponse.success(doctorDtos);
    }
    
    @GetMapping("/top-rated")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    public ApiResponse<List<DoctorDto>> getTopRatedDoctors(
            @RequestParam(defaultValue = "10") int limit) {
        List<Doctor> doctors = doctorService.findTopRatedDoctors(limit);
        List<DoctorDto> doctorDtos = doctors.stream()
            .map(DoctorDto::fromEntityBasic)
            .toList();
        return ApiResponse.success(doctorDtos);
    }

    // === VALIDATION ENDPOINTS ===
    
    @GetMapping("/check-diploma")
    public ApiResponse<Boolean> checkDiplomaAvailability(@RequestParam String diplomaNumber) {
        boolean available = doctorService.isDiplomaNumberAvailable(diplomaNumber);
        return ApiResponse.success(available);
    }
    
    @GetMapping("/check-license")
    public ApiResponse<Boolean> checkLicenseAvailability(@RequestParam String licenseNumber) {
        boolean available = doctorService.isMedicalLicenseNumberAvailable(licenseNumber);
        return ApiResponse.success(available);
    }

    // === STATISTICS ENDPOINTS ===
    
    @GetMapping("/statistics/count-verified")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getVerifiedDoctorsCount() {
        long count = doctorService.countVerifiedDoctors();
        return ApiResponse.success(count);
    }
    
    @GetMapping("/statistics/count-by-specialty")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> getDoctorsCountBySpecialty(@RequestParam String specialty) {
        long count = doctorService.countDoctorsBySpecialty(specialty);
        return ApiResponse.success(count);
    }
}