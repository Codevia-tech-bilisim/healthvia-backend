// user/repository/PatientRepository.java
package com.healthvia.platform.user.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.entity.User;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {

    // === IDENTITY BASED QUERIES ===
    
    /**
     * TC Kimlik No ile hasta bulma
     */
    Optional<Patient> findByTcKimlikNo(String tcKimlikNo);
    
    /**
     * TC Kimlik No ile aktif hasta bulma
     */
    Optional<Patient> findByTcKimlikNoAndDeletedFalse(String tcKimlikNo);

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByEmailAndDeletedFalse(String email);

    Optional<Patient> findByPasswordResetToken(String passwordResetToken);


    
    /**
     * Pasaport No ile hasta bulma
     */
    Optional<Patient> findByPassportNo(String passportNo);
    
    /**
     * Pasaport No ile aktif hasta bulma
     */
    Optional<Patient> findByPassportNoAndDeletedFalse(String passportNo);
    
    /**
     * TC Kimlik veya Pasaport ile hasta bulma
     */
    @Query("{ $or: [ {'tcKimlikNo': ?0}, {'passportNo': ?0} ], 'deleted': false }")
    Optional<Patient> findByTcKimlikNoOrPassportNo(String identityNumber);

    // === HEALTH INFORMATION QUERIES ===
    
    /**
     * Kan grubu göre hastalar
     */
    List<Patient> findByBloodTypeAndDeletedFalse(String bloodType);
    
    /**
     * Alerjisi olan hastalar
     */
    @Query("{ 'allergies': { $ne: null, $ne: '' }, 'deleted': false }")
    List<Patient> findPatientsWithAllergies();
    
    /**
     * Kronik hastalığı olan hastalar
     */
    @Query("{ 'chronicDiseases': { $ne: null, $ne: '' }, 'deleted': false }")
    List<Patient> findPatientsWithChronicDiseases();
    
    /**
     * Belirli alerjisi olan hastalar
     */
    @Query("{ 'allergies': { $regex: ?0, $options: 'i' }, 'deleted': false }")
    List<Patient> findByAllergiesContaining(String allergyType);
    
    /**
     * Belirli kronik hastalığı olan hastalar
     */
    @Query("{ 'chronicDiseases': { $regex: ?0, $options: 'i' }, 'deleted': false }")
    List<Patient> findByChronicDiseasesContaining(String disease);

    // === BMI & PHYSICAL QUERIES ===
    
    /**
     * BMI hesaplanabilir hastalar (boy ve kilo mevcut)
     */
    @Query("{ 'heightCm': { $ne: null }, 'weightKg': { $ne: null }, 'deleted': false }")
    List<Patient> findPatientsWithBMIData();
    
    /**
     * Belirli yaş aralığındaki hastalar
     */
    @Query("{ 'birthDate': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Patient> findPatientsByAgeRange(LocalDate minBirthDate, LocalDate maxBirthDate);
    
    /**
     * Boy aralığında hastalar
     */
    @Query("{ 'heightCm': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Patient> findByHeightRange(Integer minHeight, Integer maxHeight);

    // === INSURANCE QUERIES ===
    
    /**
     * Sigortalı hastalar
     */
    List<Patient> findByHasInsuranceTrueAndDeletedFalse();
    
    /**
     * Sigorta şirketi göre hastalar
     */
    List<Patient> findByInsuranceCompanyAndDeletedFalse(String insuranceCompany);
    
    /**
     * Sigortası yakında bitecek hastalar
     */
    @Query("{ 'insuranceExpiryDate': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Patient> findPatientsWithExpiringInsurance(LocalDate startDate, LocalDate endDate);
    
    /**
     * Sigortası bitmiş hastalar
     */
    @Query("{ 'insuranceExpiryDate': { $lt: ?0 }, 'deleted': false }")
    List<Patient> findPatientsWithExpiredInsurance(LocalDate currentDate);

    // === LIFESTYLE QUERIES ===
    
    /**
     * Sigara kullanım durumu göre hastalar
     */
    List<Patient> findBySmokingStatusAndDeletedFalse(Patient.SmokingStatus smokingStatus);
    
    /**
     * Alkol tüketim durumu göre hastalar
     */
    List<Patient> findByAlcoholConsumptionAndDeletedFalse(Patient.AlcoholConsumption alcoholConsumption);
    
    /**
     * Egzersiz sıklığı göre hastalar
     */
    List<Patient> findByExerciseFrequencyAndDeletedFalse(Patient.ExerciseFrequency exerciseFrequency);
    
    /**
     * Doktor cinsiyet tercihi olan hastalar
     */
    List<Patient> findByPreferredDoctorGenderAndDeletedFalse(User.Gender preferredGender);

    // === APPOINTMENT STATISTICS QUERIES ===
    
    /**
     * Toplam randevu sayısı göre hastalar
     */
    @Query("{ 'totalAppointments': { $gte: ?0 }, 'deleted': false }")
    List<Patient> findPatientsWithMinAppointments(int minAppointments);
    
    /**
     * Randevu tamamlama oranı yüksek hastalar
     */
    @Query("{ $expr: { $gte: [ { $divide: ['$completedAppointments', '$totalAppointments'] }, ?0 ] }, 'deleted': false }")
    List<Patient> findPatientsWithHighCompletionRate(double minCompletionRate);
    
    /**
     * Son randevusu belirli tarihten eski hastalar
     */
    @Query("{ 'lastAppointmentDate': { $lt: ?0 }, 'deleted': false }")
    List<Patient> findInactivePatientsSince(LocalDate lastAppointmentBefore);
    
    /**
     * Hiç randevusu olmayan hastalar
     */
    @Query("{ $or: [ {'totalAppointments': null}, {'totalAppointments': 0} ], 'deleted': false }")
    List<Patient> findPatientsWithoutAppointments();

    // === LOCATION BASED QUERIES ===
    
    /**
     * İl ve ilçe göre hastalar
     */
    List<Patient> findByProvinceAndDistrictAndDeletedFalse(String province, String district);
    
    /**
     * Posta kodu göre hastalar
     */
    List<Patient> findByPostalCodeAndDeletedFalse(String postalCode);
    
    /**
     * Doğum yeri göre hastalar
     */
    List<Patient> findByBirthPlaceContainingIgnoreCaseAndDeletedFalse(String birthPlace);

    // === EMERGENCY CONTACT QUERIES ===
    
    /**
     * Acil durum iletişimi olan hastalar
     */
    @Query("{ 'emergencyContactName': { $ne: null, $ne: '' }, 'emergencyContactPhone': { $ne: null, $ne: '' }, 'deleted': false }")
    List<Patient> findPatientsWithEmergencyContact();
    
    /**
     * Acil durum iletişimi olmayan hastalar
     */
    @Query("{ $or: [ {'emergencyContactName': null}, {'emergencyContactName': ''}, {'emergencyContactPhone': null}, {'emergencyContactPhone': ''} ], 'deleted': false }")
    List<Patient> findPatientsWithoutEmergencyContact();

    // === SEARCH QUERIES ===
    
    /**
     * İsim, TC Kimlik veya telefon ile hasta arama
     */
    @Query("{ $or: [ " +
           "{'firstName': {$regex: ?0, $options: 'i'}}, " +
           "{'lastName': {$regex: ?0, $options: 'i'}}, " +
           "{'tcKimlikNo': ?0}, " +
           "{'phone': ?0}, " +
           "{'email': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<Patient> searchPatients(String searchTerm, Pageable pageable);
    
    /**
     * Sağlık durumu arama (alerji, kronik hastalık)
     */
    @Query("{ $or: [ " +
           "{'allergies': {$regex: ?0, $options: 'i'}}, " +
           "{'chronicDiseases': {$regex: ?0, $options: 'i'}}, " +
           "{'currentMedications': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    List<Patient> searchByHealthConditions(String condition);

    // === EXISTENCE CHECKS ===
    
    /**
     * TC Kimlik No varlık kontrolü
     */
    boolean existsByTcKimlikNoAndDeletedFalse(String tcKimlikNo);
    
    /**
     * Pasaport No varlık kontrolü
     */
    boolean existsByPassportNoAndDeletedFalse(String passportNo);


    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);


    // === COUNT QUERIES ===
    
    /**
     * Sigortalı hasta sayısı
     */
    long countByHasInsuranceTrueAndDeletedFalse();
    
    /**
     * Belirli il ve ilçedeki hasta sayısı
     */
    long countByProvinceAndDistrictAndDeletedFalse(String province, String district);
    
    /**
     * Sağlık sorunu olan hasta sayısı
     */
    @Query("{ $or: [ " +
           "{'allergies': { $ne: null, $ne: '' }}, " +
           "{'chronicDiseases': { $ne: null, $ne: '' }} " +
           "], 'deleted': false }")
    long countPatientsWithHealthIssues();

    // === COMPLEX ANALYTICS QUERIES ===
    
    /**
     * Yaş grubuna göre hasta dağılımı için
     */
    @Query("{ 'birthDate': { $gte: ?0, $lt: ?1 }, 'deleted': false }")
    long countPatientsByAgeGroup(LocalDate minBirthDate, LocalDate maxBirthDate);
    
    /**
     * En çok randevusu olan hastalar (Top N)
     */
    @Query("{ 'deleted': false }")
    List<Patient> findTopPatientsByAppointmentCount(Pageable pageable);
}