package com.healthvia.platform.auth.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.auth.dto.AuthResponse;
import com.healthvia.platform.auth.dto.LoginRequest;
import com.healthvia.platform.auth.dto.RegisterAdminRequest;
import com.healthvia.platform.auth.dto.RegisterDoctorRequest;
import com.healthvia.platform.auth.dto.RegisterRequest;
import com.healthvia.platform.auth.security.JwtTokenProvider;
import com.healthvia.platform.auth.security.UserPrincipal;
import com.healthvia.platform.auth.service.AuthService;
import com.healthvia.platform.common.constants.ErrorCodes;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.common.exception.BusinessException;
import com.healthvia.platform.common.util.TcKimlikNoValidator;
import com.healthvia.platform.doctor.entity.Doctor;
import com.healthvia.platform.doctor.repository.DoctorRepository;
import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.entity.User;
import com.healthvia.platform.user.repository.PatientRepository;
import com.healthvia.platform.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse registerPatient(RegisterRequest request) {
        log.debug("Registering patient with email: {}", request.getEmail());
        
        // Validate common fields
        validateCommonFields(request);
        
        // Create Patient entity
        Patient patient = Patient.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.PATIENT)
            .status(UserStatus.ACTIVE)
            .gender(request.getGender())
            .birthDate(request.getBirthDate())
            .province(request.getProvince())
            .district(request.getDistrict())
            .emailVerified(true)
            .phoneVerified(false)
            .gdprConsent(request.getGdprConsent())
            .gdprConsentDate(LocalDateTime.now())
            .tcKimlikNo(request.getTcKimlikNo())
            .passportNo(request.getPassportNo())
            .birthPlace(request.getBirthPlace())
            .failedLoginAttempts(0)
            .profileCompletionRate(30)
            .build();

        if (request.getTcKimlikNo() != null && !request.getTcKimlikNo().isEmpty()) {
            if (!TcKimlikNoValidator.isValid(request.getTcKimlikNo())) {
                throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR, 
                    "Geçersiz TC Kimlik Numarası"
                );
            }
            // Formatlanmış TC'yi patient nesnesine set et
            patient.setTcKimlikNo(
                TcKimlikNoValidator.format(request.getTcKimlikNo())
            );
        } else {
            patient.setTcKimlikNo(request.getTcKimlikNo());
        }
        
        // Save directly with PatientRepository
        Patient savedPatient = patientRepository.save(patient);
        log.info("Patient created and activated with ID: {}", savedPatient.getId());
        
        // Create auth response
        AuthResponse response = createAuthResponse(savedPatient);
        response.setMessage("Hasta kaydı tamamlandı - Giriş yapabilirsiniz");
        response.setRequiresAction(false);
        
        return response;
    }

    @Override
    public AuthResponse registerDoctor(RegisterDoctorRequest request) {
        log.debug("Registering doctor with email: {}", request.getEmail());
        
        validateCommonFields(request);
        validateDoctorFields(request);
        
        // Create Doctor entity
        Doctor doctor = Doctor.builder()
            // User fields
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.DOCTOR)
            .status(UserStatus.ACTIVE)
            .gender(request.getGender())
            .birthDate(request.getBirthDate())
            .province(request.getProvince())
            .district(request.getDistrict())
            .emailVerified(true)
            .phoneVerified(false)
            .gdprConsent(request.getGdprConsent())
            .gdprConsentDate(LocalDateTime.now())
            .failedLoginAttempts(0)
            .profileCompletionRate(40)
            
            // Doctor specific fields
            .diplomaNumber(request.getDiplomaNumber())
            .medicalLicenseNumber(request.getMedicalLicenseNumber())
            .medicalSchool(request.getMedicalSchool())
            .graduationYear(request.getGraduationYear())
            .primarySpecialty(request.getPrimarySpecialty())
            .specialtySchool(request.getSpecialtySchool())
            .specialtyCompletionYear(request.getSpecialtyCompletionYear())
            .yearsOfExperience(request.getYearsOfExperience())
            .currentHospital(request.getCurrentHospital())
            .currentClinic(request.getCurrentClinic())
            .verificationStatus(Doctor.VerificationStatus.PENDING)
            .isAcceptingNewPatients(false)
            .build();
        
        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Doctor created and activated with ID: {}", savedDoctor.getId());
        
        AuthResponse response = createAuthResponse(savedDoctor);
        response.setMessage("Doktor kaydı tamamlandı - Admin onayı bekleniyor");
        response.setRequiresAction(false);
        
        return response;
    }

    @Override
    public AuthResponse registerAdmin(RegisterAdminRequest request) {
        log.debug("Registering admin with email: {}", request.getEmail());
        
        validateCommonFields(request);
        validateAdminFields(request);
        
        // Create Admin entity
        Admin admin = Admin.builder()
            // User fields
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.ADMIN)
            .status(UserStatus.ACTIVE)
            .gender(request.getGender())
            .birthDate(request.getBirthDate())
            .province(request.getProvince())
            .district(request.getDistrict())
            .emailVerified(true)
            .phoneVerified(false)
            .gdprConsent(request.getGdprConsent())
            .gdprConsentDate(LocalDateTime.now())
            .failedLoginAttempts(0)
            .profileCompletionRate(50)
            
            // Admin specific fields
            .department(request.getDepartment())
            .jobTitle(request.getJobTitle())
            .employeeId(request.getEmployeeId())
            .adminLevel(request.getAdminLevel() != null ? request.getAdminLevel() : Admin.AdminLevel.STANDARD)
            .permissions(request.getPermissions())
            .supervisorId(request.getSupervisorId())
            .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDateTime.now())
            .canManageUsers(request.getCanManageUsers() != null ? request.getCanManageUsers() : true)
            .canManageDoctors(Boolean.TRUE.equals(request.getCanManageDoctors()))
            .canManageClinics(Boolean.TRUE.equals(request.getCanManageClinics()))
            .canViewReports(Boolean.TRUE.equals(request.getCanViewReports()))
            .canManageSystem(request.getCanManageSystem() != null ? request.getCanManageSystem() : false)
            .build();
        
        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin created successfully with ID: {}", savedAdmin.getId());
        
        AuthResponse response = createAuthResponse(savedAdmin);
        response.setMessage("Admin hesabı oluşturuldu");
        response.setRequiresAction(false);
        
        return response;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for username: {}", request.getUsername());
        
        // Find user - önce UserRepository, sonra PatientRepository, sonra DoctorRepository, son AdminRepository
        User user = userRepository.findByEmailOrPhone(request.getUsername())
            .or(() -> patientRepository.findByEmail(request.getUsername()).map(patient -> (User) patient))
            .or(() -> doctorRepository.findByEmail(request.getUsername()).map(doctor -> (User) doctor))
            .or(() -> adminRepository.findByEmail(request.getUsername()).map(admin -> (User) admin))
            .orElseThrow(() -> new BusinessException(ErrorCodes.INVALID_CREDENTIALS));
        
        // Check account status
        if (user.isAccountLocked()) {
            throw new BusinessException(ErrorCodes.ACCOUNT_LOCKED);
        }
        
        // Only check if user is active (not suspended/deleted)
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCodes.ACCOUNT_LOCKED, "Account suspended");
        }
        
        if (user.getStatus() == UserStatus.DELETED || user.isDeleted()) {
            throw new BusinessException(ErrorCodes.USER_NOT_FOUND, "Account not found");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.incrementFailedLoginAttempts();
            saveUserToCorrectRepository(user);
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS);
        }
        
        // Update last login
        user.updateLastLogin();
        saveUserToCorrectRepository(user);
        
        AuthResponse response = createAuthResponse(user);
        response.setMessage("Giriş başarılı");
        response.setRequiresAction(false);
        
        return response;
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCodes.TOKEN_INVALID);
        }
        
        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        
        // Try to find user in all repositories
        User user = userRepository.findById(userId)
            .or(() -> patientRepository.findById(userId).map(patient -> (User) patient))
            .or(() -> doctorRepository.findById(userId).map(doctor -> (User) doctor))
            .or(() -> adminRepository.findById(userId).map(admin -> (User) admin))
            .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND));
        
        return createAuthResponse(user);
    }

    @Override
    public void verifyEmail(String token) {
        log.info("Email verification requested with token: {}", token);
        throw new UnsupportedOperationException("Email verification will be implemented with email service");
    }

    @Override
    public void forgotPassword(String email) {
        // Find user in any repository
        User user = userRepository.findByEmail(email)
            .or(() -> patientRepository.findByEmail(email).map(patient -> (User) patient))
            .or(() -> doctorRepository.findByEmail(email).map(doctor -> (User) doctor))
            .or(() -> adminRepository.findByEmail(email).map(admin -> (User) admin))
            .orElseThrow(() -> new BusinessException(ErrorCodes.USER_NOT_FOUND));
        
        log.info("Password reset requested for user: {}", user.getId());
        throw new UnsupportedOperationException("Password reset will be implemented with email service");
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset attempted with token: {}", token);
        throw new UnsupportedOperationException("Password reset will be implemented with email service");
    }

    // === PRIVATE HELPER METHODS ===

    private void validateCommonFields(RegisterRequest request) {
        // Email uniqueness check across all repositories
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail()) ||
            patientRepository.existsByEmail(request.getEmail()) ||
            doctorRepository.existsByEmail(request.getEmail()) ||
            adminRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCodes.USER_ALREADY_EXISTS, "Email already exists: " + request.getEmail());
        }
        
        // Phone uniqueness check across all repositories
        if (userRepository.existsByPhoneAndDeletedFalse(request.getPhone()) ||
            patientRepository.existsByPhone(request.getPhone()) ||
            doctorRepository.existsByPhone(request.getPhone()) ||
            adminRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCodes.USER_ALREADY_EXISTS, "Phone already exists: " + request.getPhone());
        }
        
        // GDPR consent check
        if (!Boolean.TRUE.equals(request.getGdprConsent())) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "GDPR consent is required");
        }
    }

    private void validateDoctorFields(RegisterDoctorRequest request) {
        if (request.getDiplomaNumber() == null || request.getDiplomaNumber().trim().isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Diploma number is required");
        }
        
        if (request.getMedicalLicenseNumber() == null || request.getMedicalLicenseNumber().trim().isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Medical license number is required");
        }
        
        if (request.getPrimarySpecialty() == null || request.getPrimarySpecialty().trim().isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Primary specialty is required");
        }
        
        // Check uniqueness
        if (doctorRepository.existsByDiplomaNumberAndDeletedFalse(request.getDiplomaNumber())) {
            throw new BusinessException(ErrorCodes.USER_ALREADY_EXISTS, "Diploma number already exists");
        }
        
        if (doctorRepository.existsByMedicalLicenseNumberAndDeletedFalse(request.getMedicalLicenseNumber())) {
            throw new BusinessException(ErrorCodes.USER_ALREADY_EXISTS, "Medical license number already exists");
        }
    }

    private void validateAdminFields(RegisterAdminRequest request) {
        if (request.getDepartment() == null || request.getDepartment().trim().isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Department is required");
        }
        
        if (request.getEmployeeId() == null || request.getEmployeeId().trim().isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Employee ID is required");
        }
        
        // Check uniqueness
        if (adminRepository.existsByEmployeeIdAndDeletedFalse(request.getEmployeeId())) {
            throw new BusinessException(ErrorCodes.USER_ALREADY_EXISTS, "Employee ID already exists");
        }
    }

    private void saveUserToCorrectRepository(User user) {
        if (user instanceof Patient) {
            patientRepository.save((Patient) user);
        } else if (user instanceof Doctor) {
            doctorRepository.save((Doctor) user);
        } else if (user instanceof Admin) {
            adminRepository.save((Admin) user);
        } else {
            userRepository.save(user);
        }
    }

    private AuthResponse createAuthResponse(User user) {
        // Create UserPrincipal
        UserPrincipal userPrincipal = createUserPrincipal(user);
        
        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        
        log.info("Authentication successful for user: {}", user.getId());
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(900000) // 15 minutes
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .status(user.getStatus())
            .emailVerified(user.getEmailVerified())
            .phoneVerified(user.getPhoneVerified())
            .lastLoginDate(user.getLastLoginDate())
            .build();
    }

    private UserPrincipal createUserPrincipal(User user) {
        return UserPrincipal.builder()
            .id(user.getId())
            .email(user.getEmail())
            .password(user.getPassword())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .status(user.getStatus())
            .emailVerified(user.getEmailVerified())
            .build();
    }
}