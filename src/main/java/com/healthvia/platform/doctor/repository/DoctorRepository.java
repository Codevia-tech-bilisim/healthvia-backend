// user/repository/DoctorRepository.java
package com.healthvia.platform.doctor.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.doctor.entity.Doctor;

@Repository
public interface DoctorRepository extends MongoRepository<Doctor, String> {

    // === PROFESSIONAL IDENTITY QUERIES ===
    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByEmailAndDeletedFalse(String email);
    Optional<Doctor> findByPasswordResetToken(String passwordResetToken);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    /**
     * Diploma numarası ile doktor bulma
     */
    Optional<Doctor> findByDiplomaNumber(String diplomaNumber);
    
    /**
     * Tabip Odası sicil numarası ile doktor bulma
     */
    Optional<Doctor> findByMedicalLicenseNumber(String medicalLicenseNumber);
    
    /**
     * Diploma numarası ile aktif doktor bulma
     */
    Optional<Doctor> findByDiplomaNumberAndDeletedFalse(String diplomaNumber);

    // === SPECIALTY BASED QUERIES ===
    
    /**
     * Ana uzmanlık alanına göre doktorlar
     */
    List<Doctor> findByPrimarySpecialtyAndDeletedFalse(String primarySpecialty);
    
    /**
     * Uzmanlık alanı içeren doktorlar (ana veya yan dal)
     */
    @Query("{ $or: [ " +
           "{'primarySpecialty': {$regex: ?0, $options: 'i'}}, " +
           "{'subspecialties': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    List<Doctor> findBySpecialtyContaining(String specialty);
    
    /**
     * Birden fazla uzmanlık alanı olan doktorlar
     */
    @Query("{ 'subspecialties': { $exists: true, $not: {$size: 0} }, 'deleted': false }")
    List<Doctor> findDoctorsWithMultipleSpecialties();
    
    /**
     * Belirli tıbbi ilgi alanına sahip doktorlar
     */
    @Query("{ 'medicalInterests': { $in: [?0] }, 'deleted': false }")
    List<Doctor> findByMedicalInterest(String interest);

    // === EDUCATION & EXPERIENCE QUERIES ===
    
    /**
     * Mezun olduğu tıp fakültesine göre doktorlar
     */
    List<Doctor> findByMedicalSchoolContainingIgnoreCaseAndDeletedFalse(String medicalSchool);
    
    /**
     * Mezuniyet yılı aralığında doktorlar
     */
    @Query("{ 'graduationYear': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Doctor> findByGraduationYearBetween(Integer startYear, Integer endYear);
    
    /**
     * Minimum deneyim yılına sahip doktorlar
     */
    @Query("{ 'yearsOfExperience': { $gte: ?0 }, 'deleted': false }")
    List<Doctor> findByMinExperience(Integer minYears);
    
    /**
     * Deneyim aralığında doktorlar
     */
    @Query("{ 'yearsOfExperience': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Doctor> findByExperienceRange(Integer minYears, Integer maxYears);

    // === LOCATION & WORKPLACE QUERIES ===
    
    /**
     * Şu an çalıştığı hastaneye göre doktorlar
     */
    List<Doctor> findByCurrentHospitalContainingIgnoreCaseAndDeletedFalse(String hospital);
    
    /**
     * Şu an çalıştığı kliniğe göre doktorlar
     */
    List<Doctor> findByCurrentClinicContainingIgnoreCaseAndDeletedFalse(String clinic);
    
    /**
     * İl ve ilçede çalışan doktorlar
     */
    List<Doctor> findByProvinceAndDistrictAndDeletedFalse(String province, String district);
    
    /**
     * Belirli ilde çalışan doktorlar
     */
    List<Doctor> findByProvinceAndDeletedFalse(String province);

    // === AVAILABILITY QUERIES ===
    
    /**
     * Yeni hasta kabul eden doktorlar
     */
    @Query("{ 'isAcceptingNewPatients': true, 'deleted': false }")
    List<Doctor> findAvailableDoctors();
    
    /**
     * Acil durum müsait doktorlar
     */
    @Query("{ 'isAvailableForEmergencies': true, 'deleted': false }")
    List<Doctor> findEmergencyAvailableDoctors();
    
    /**
     * Belirli günde çalışan doktorlar
     */
    @Query("{ 'workingDays': { $in: [?0] }, 'deleted': false }")
    List<Doctor> findDoctorsWorkingOnDay(String dayOfWeek);
    
    /**
     * Belirli saat aralığında çalışan doktorlar
     */
    @Query("{ 'workingHoursStart': { $lte: ?0 }, 'workingHoursEnd': { $gte: ?1 }, 'deleted': false }")
    List<Doctor> findDoctorsAvailableInTimeRange(LocalTime startTime, LocalTime endTime);

    // === CONSULTATION & PRICING QUERIES ===
    
    /**
     * Belirli ücret aralığındaki doktorlar
     */
    @Query("{ 'consultationFee': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Doctor> findByConsultationFeeRange(BigDecimal minFee, BigDecimal maxFee);
    
    /**
     * Maksimum ücret altındaki doktorlar
     */
    @Query("{ 'consultationFee': { $lte: ?0 }, 'deleted': false }")
    List<Doctor> findByMaxConsultationFee(BigDecimal maxFee);
    
    /**
     * Konsültasyon türüne göre doktorlar
     */
    @Query("{ 'consultationTypes': { $in: [?0] }, 'deleted': false }")
    List<Doctor> findByConsultationType(Doctor.ConsultationType consultationType);
    
    /**
     * Online konsültasyon veren doktorlar
     */
    @Query("{ 'consultationTypes': { $in: ['ONLINE'] }, 'deleted': false }")
    List<Doctor> findOnlineConsultationDoctors();

    // === RATING & REVIEW QUERIES ===
    
    /**
     * Minimum rating'e sahip doktorlar
     */
    @Query("{ 'averageRating': { $gte: ?0 }, 'deleted': false }")
    List<Doctor> findByMinRating(Double minRating);
    
    /**
     * Yüksek rating'li doktorlar (4.0+)
     */
    @Query("{ 'averageRating': { $gte: 4.0 }, 'deleted': false }")
    List<Doctor> findHighRatedDoctors();
    
    /**
     * En çok değerlendirilen doktorlar
     */
    @Query("{ 'totalReviews': { $gte: ?0 }, 'deleted': false }")
    List<Doctor> findDoctorsWithMinReviews(Integer minReviews);
    
    /**
     * Rating'e göre sıralı doktorlar
     */
    @Query("{ 'deleted': false }")
    List<Doctor> findAllOrderByAverageRatingDesc(Pageable pageable);

    // === VERIFICATION & STATUS QUERIES ===
    
    /**
     * Doğrulanmış doktorlar
     */
    @Query("{ 'verificationStatus': 'VERIFIED', 'deleted': false }")
    List<Doctor> findVerifiedDoctors();
    
    /**
     * Onay bekleyen doktorlar
     */
    @Query("{ 'verificationStatus': 'PENDING', 'deleted': false }")
    List<Doctor> findPendingVerificationDoctors();
    
    /**
     * Verification status'e göre doktorlar
     */
    List<Doctor> findByVerificationStatusAndDeletedFalse(Doctor.VerificationStatus status);
    
    /**
     * Belirli tarihten sonra doğrulanan doktorlar
     */
    @Query("{ 'verificationDate': { $gte: ?0 }, 'deleted': false }")
    List<Doctor> findDoctorsVerifiedAfter(LocalDate verificationDate);

    // === LANGUAGE QUERIES ===
    
    /**
     * Belirli dil konuşan doktorlar
     */
    @Query("{ 'languagesSpoken': { $in: [?0] }, 'deleted': false }")
    List<Doctor> findByLanguageSpoken(String language);
    
    /**
     * Çoklu dil bilen doktorlar
     */
    @Query("{ 'languagesSpoken': { $exists: true, $size: { $gte: 2 } }, 'deleted': false }")
    List<Doctor> findMultilingualDoctors();

    // === APPOINTMENT STATISTICS QUERIES ===
    
    /**
     * En çok randevu alan doktorlar
     */
    @Query("{ 'totalAppointments': { $gte: ?0 }, 'deleted': false }")
    List<Doctor> findDoctorsWithMinAppointments(Integer minAppointments);
    
    /**
     * Yüksek tamamlama oranına sahip doktorlar
     */
    @Query("{ $expr: { $gte: [ { $divide: ['$completedAppointments', '$totalAppointments'] }, ?0 ] }, 'deleted': false }")
    List<Doctor> findDoctorsWithHighCompletionRate(Double minCompletionRate);
    
    /**
     * En çok hasta tedavi eden doktorlar
     */
    @Query("{ 'patientsTreated': { $gte: ?0 }, 'deleted': false }")
    List<Doctor> findDoctorsWithMinPatients(Integer minPatients);

    // === SEARCH QUERIES ===
    
    /**
     * Doktor arama - isim, uzmanlık, hastane
     */
    @Query("{ $or: [ " +
           "{'firstName': {$regex: ?0, $options: 'i'}}, " +
           "{'lastName': {$regex: ?0, $options: 'i'}}, " +
           "{'primarySpecialty': {$regex: ?0, $options: 'i'}}, " +
           "{'currentHospital': {$regex: ?0, $options: 'i'}}, " +
           "{'currentClinic': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<Doctor> searchDoctors(String searchTerm, Pageable pageable);
    
    /**
     * Gelişmiş doktor arama
     */
    @Query("{ $and: [ " +
           "{ $or: [ " +
           "  {'primarySpecialty': {$regex: ?0, $options: 'i'}}, " +
           "  {'subspecialties': {$regex: ?0, $options: 'i'}} " +
           "]}, " +
           "{'province': ?1}, " +
           "{'isAcceptingNewPatients': true}, " +
           "{'verificationStatus': 'VERIFIED'}, " +
           "{'deleted': false} " +
           "]}")
    List<Doctor> findAvailableDoctorsBySpecialtyAndLocation(String specialty, String province);

    // === COMPLEX FILTERING QUERIES ===
    
    /**
     * Filtreli doktor arama
     */
    @Query("{ $and: [ " +
           "{ $or: [ " +
           "  {?0: null}, " +
           "  {'primarySpecialty': {$regex: ?0, $options: 'i'}} " +
           "]}, " +
           "{ $or: [ " +
           "  {?1: null}, " +
           "  {'province': ?1} " +
           "]}, " +
           "{ $or: [ " +
           "  {?2: null}, " +
           "  {'averageRating': {$gte: ?2}} " +
           "]}, " +
           "{ $or: [ " +
           "  {?3: null}, " +
           "  {'consultationFee': {$lte: ?3}} " +
           "]}, " +
           "{'isAcceptingNewPatients': true}, " +
           "{'verificationStatus': 'VERIFIED'}, " +
           "{'deleted': false} " +
           "]}")
    Page<Doctor> findDoctorsWithFilters(
        String specialty, 
        String province, 
        Double minRating, 
        BigDecimal maxFee, 
        Pageable pageable
    );




    // === EXISTENCE CHECKS ===
    
    /**
     * Diploma numarası varlık kontrolü
     */
    boolean existsByDiplomaNumberAndDeletedFalse(String diplomaNumber);
    
    /**
     * Tabip Odası sicil numarası varlık kontrolü
     */
    boolean existsByMedicalLicenseNumberAndDeletedFalse(String medicalLicenseNumber);

    // === COUNT QUERIES ===
    
    /**
     * Uzmanlık alanına göre doktor sayısı
     */
    long countByPrimarySpecialtyAndDeletedFalse(String specialty);
    
    /**
     * İle göre doktor sayısı
     */
    long countByProvinceAndDeletedFalse(String province);
    
    /**
     * Doğrulanmış doktor sayısı
     */
    @Query("{ 'verificationStatus': 'VERIFIED', 'deleted': false }")
    long countVerifiedDoctors();
    
    /**
     * Yeni hasta kabul eden doktor sayısı
     */
    @Query("{ 'isAcceptingNewPatients': true, 'deleted': false }")
    long countAvailableDoctors();

    // === ANALYTICS QUERIES ===
    
    /**
     * En popüler uzmanlık alanları için
     */
    @Query("{ 'deleted': false }")
    List<Doctor> findAllForSpecialtyAnalytics();
    
    /**
     * Ücret ortalaması için belirli uzmanlık
     */
    @Query("{ 'primarySpecialty': ?0, 'consultationFee': { $ne: null }, 'deleted': false }")
    List<Doctor> findDoctorsForFeeAnalysis(String specialty);
    
    /**
     * En başarılı doktorlar (rating + deneyim)
     */
    @Query("{ 'averageRating': { $gte: 4.0 }, 'yearsOfExperience': { $gte: 5 }, 'deleted': false }")
    List<Doctor> findTopPerformingDoctors(Pageable pageable);

    // === TIME-BASED QUERIES ===
    
    /**
     * Bu ay katılan doktorlar
     */
    @Query("{ 'createdAt': { $gte: ?0, $lt: ?1 }, 'deleted': false }")
    List<Doctor> findDoctorsJoinedBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Yakın zamanda aktif olmayan doktorlar
     */
    @Query("{ 'lastLoginDate': { $lt: ?0 }, 'deleted': false }")
    List<Doctor> findInactiveDoctorsSince(LocalDate lastLoginBefore);

    // === CERTIFICATION QUERIES ===
    
    /**
     * Aktif sertifikası olan doktorlar
     */
    @Query("{ 'certifications': { $elemMatch: { 'isValid': true, 'expiryDate': { $gt: ?0 } } }, 'deleted': false }")
    List<Doctor> findDoctorsWithValidCertifications(LocalDate currentDate);
    
    /**
     * Sertifikası yakında bitecek doktorlar
     */
    @Query("{ 'certifications': { $elemMatch: { 'expiryDate': { $gte: ?0, $lte: ?1 } } }, 'deleted': false }")
    List<Doctor> findDoctorsWithExpiringCertifications(LocalDate startDate, LocalDate endDate);

    // === RECOMMENDATION QUERIES ===
    
    /**
     * Hasta tercihleri için doktor önerisi
     */
    @Query("{ $and: [ " +
           "{'primarySpecialty': ?0}, " +
           "{'province': ?1}, " +
           "{'gender': ?2}, " +
           "{'languagesSpoken': { $in: [?3] }}, " +
           "{'isAcceptingNewPatients': true}, " +
           "{'averageRating': { $gte: 3.5 }}, " +
           "{'verificationStatus': 'VERIFIED'}, " +
           "{'deleted': false} " +
           "]}")
    List<Doctor> findRecommendedDoctors(
        String specialty, 
        String province, 
        String gender, 
        String language, 
        Pageable pageable
    );




    
}