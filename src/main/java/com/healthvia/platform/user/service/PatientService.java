// user/service/PatientService.java
package com.healthvia.platform.user.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.user.entity.Patient;
import com.healthvia.platform.user.entity.User;

public interface PatientService {

    // === BASIC CRUD OPERATIONS ===
    
    /**
     * Hasta oluştur
     */
    Patient createPatient(Patient patient);
    
    /**
     * Hasta güncelle
     */
    Patient updatePatient(String id, Patient patient);
    
    /**
     * Hasta bul (ID ile)
     */
    Optional<Patient> findById(String id);
    
    /**
     * Hasta sil (soft delete)
     */
    void deletePatient(String id, String deletedBy);
    
    /**
     * Tüm hastaları listele
     */
    Page<Patient> findAll(Pageable pageable);

    // === IDENTITY BASED OPERATIONS ===
    
    /**
     * TC Kimlik No ile hasta bul
     */
    Optional<Patient> findByTcKimlikNo(String tcKimlikNo);
    
    /**
     * Pasaport No ile hasta bul
     */
    Optional<Patient> findByPassportNo(String passportNo);
    
    /**
     * TC Kimlik veya Pasaport ile hasta bul
     */
    Optional<Patient> findByIdentityNumber(String identityNumber);
    
    /**
     * TC Kimlik No benzersizlik kontrolü
     */
    boolean isTcKimlikNoAvailable(String tcKimlikNo);
    
    /**
     * Pasaport No benzersizlik kontrolü
     */
    boolean isPassportNoAvailable(String passportNo);

    // === HEALTH INFORMATION MANAGEMENT ===
    
    /**
     * Sağlık bilgilerini güncelle
     */
    Patient updateHealthInformation(String patientId, String allergies, String chronicDiseases, 
                                  String currentMedications, String familyMedicalHistory);
    
    /**
     * Kan grubu güncelle
     */
    Patient updateBloodType(String patientId, String bloodType);
    
    /**
     * Boy ve kilo güncelle
     */
    Patient updatePhysicalMeasurements(String patientId, Integer heightCm, Double weightKg);
    
    /**
     * BMI hesapla
     */
    Double calculateBMI(String patientId);

    /**
     * TC Kimlik No'yu maskeler
     */
    String getMaskedTcKimlikNo(String patientId);
    
    /**
     * BMI kategorisi belirle
     */
    String getBMICategory(String patientId);

    // === INSURANCE MANAGEMENT ===
    
    /**
     * Sigorta bilgileri güncelle
     */
    Patient updateInsuranceInformation(String patientId, String insuranceCompany, 
                                     String policyNumber, LocalDate expiryDate);
    
    /**
     * Sigorta durumu güncelle
     */
    Patient updateInsuranceStatus(String patientId, boolean hasInsurance);
    
    /**
     * Sigortası yakında bitecek hastaları bul
     */
    List<Patient> findPatientsWithExpiringInsurance(int daysBeforeExpiry);
    
    /**
     * Sigortası bitmiş hastaları bul
     */
    List<Patient> findPatientsWithExpiredInsurance();

    // === EMERGENCY CONTACT MANAGEMENT ===
    
    /**
     * Acil durum iletişim bilgilerini güncelle
     */
    Patient updateEmergencyContact(String patientId, String contactName, 
                                 String contactPhone, String relationship);
    
    /**
     * Acil durum iletişimi olan hastaları bul
     */
    List<Patient> findPatientsWithEmergencyContact();
    
    /**
     * Acil durum iletişimi olmayan hastaları bul
     */
    List<Patient> findPatientsWithoutEmergencyContact();

    // === LIFESTYLE MANAGEMENT ===
    
    /**
     * Lifestyle bilgilerini güncelle
     */
    Patient updateLifestyleInformation(String patientId, Patient.SmokingStatus smokingStatus,
                                     Patient.AlcoholConsumption alcoholConsumption,
                                     Patient.ExerciseFrequency exerciseFrequency);
    
    /**
     * Doktor cinsiyet tercihini güncelle
     */
    Patient updatePreferredDoctorGender(String patientId, User.Gender preferredGender);

    // === SEARCH & FILTER OPERATIONS ===
    
    /**
     * Hasta arama (isim, TC Kimlik, telefon)
     */
    Page<Patient> searchPatients(String searchTerm, Pageable pageable);
    
    /**
     * Kan grubu göre hastalar
     */
    List<Patient> findByBloodType(String bloodType);
    
    /**
     * Alerjisi olan hastalar
     */
    List<Patient> findPatientsWithAllergies();
    
    /**
     * Belirli alerjisi olan hastalar
     */
    List<Patient> findByAllergyType(String allergyType);
    
    /**
     * Kronik hastalığı olan hastalar
     */
    List<Patient> findPatientsWithChronicDiseases();
    
    /**
     * Belirli kronik hastalığı olan hastalar
     */
    List<Patient> findByChronicDisease(String disease);
    
    /**
     * Sağlık durumu arama
     */
    List<Patient> searchByHealthConditions(String condition);

    // === LOCATION BASED OPERATIONS ===
    
    /**
     * Lokasyon göre hastalar
     */
    List<Patient> findByLocation(String province, String district);
    
    /**
     * Posta kodu göre hastalar
     */
    List<Patient> findByPostalCode(String postalCode);
    
    /**
     * Doğum yeri göre hastalar
     */
    List<Patient> findByBirthPlace(String birthPlace);

    // === AGE & DEMOGRAPHICS ===
    
    /**
     * Yaş aralığında hastalar
     */
    List<Patient> findPatientsByAgeRange(int minAge, int maxAge);
    
    /**
     * Belirli yaş grubundaki hasta sayısı
     */
    long countPatientsByAgeGroup(int minAge, int maxAge);
    
    /**
     * Cinsiyet göre hasta sayısı
     */
    long countPatientsByGender(User.Gender gender);

    // === APPOINTMENT STATISTICS ===
    
    /**
     * Randevu istatistiklerini güncelle
     */
    Patient updateAppointmentStatistics(String patientId, int totalAppointments, 
                                       int completedAppointments, int cancelledAppointments);
    
    /**
     * Son randevu tarihini güncelle
     */
    Patient updateLastAppointmentDate(String patientId, LocalDate lastAppointmentDate);
    
    /**
     * Randevu tamamlama oranı hesapla
     */
    Double calculateAppointmentCompletionRate(String patientId);
    
    /**
     * Hiç randevusu olmayan hastalar
     */
    List<Patient> findPatientsWithoutAppointments();
    
    /**
     * Aktif olmayan hastalar (son randevusu eski)
     */
    List<Patient> findInactivePatients(LocalDate lastAppointmentBefore);
    
    /**
     * En çok randevusu olan hastalar
     */
    List<Patient> findTopPatientsByAppointments(int limit);

    // === ANALYTICS & REPORTING ===
    
    /**
     * Sigortalı hasta sayısı
     */
    long countPatientsWithInsurance();
    
    /**
     * Sağlık sorunu olan hasta sayısı
     */
    long countPatientsWithHealthIssues();
    
    /**
     * İl ve ilçe göre hasta dağılımı
     */
    long countPatientsByLocation(String province, String district);
    
    /**
     * BMI verisi olan hasta sayısı
     */
    long countPatientsWithBMIData();
    
    /**
     * Lifestyle istatistikleri
     */
    long countPatientsBySmokingStatus(Patient.SmokingStatus status);
    long countPatientsByAlcoholConsumption(Patient.AlcoholConsumption consumption);
    long countPatientsByExerciseFrequency(Patient.ExerciseFrequency frequency);

    // === HEALTH ALERTS & NOTIFICATIONS ===
    
    /**
     * Sağlık uyarısı gereken hastalar
     */
    List<Patient> findPatientsRequiringHealthAlerts();
    
    /**
     * Check-up hatırlatması gereken hastalar
     */
    List<Patient> findPatientsForCheckupReminder(int monthsSinceLastAppointment);
    
    /**
     * Obez hastalar (BMI > 30)
     */
    List<Patient> findObesePatients();
    
    /**
     * Risk grubu hastalar (yaş, kronik hastalık vs.)
     */
    List<Patient> findHighRiskPatients();

    // === BULK OPERATIONS ===
    
    /**
     * Toplu hasta oluştur
     */
    List<Patient> createPatients(List<Patient> patients);
    
    /**
     * Toplu sağlık bilgisi güncelleme
     */
    void updatePatientsHealthInfo(List<String> patientIds, String healthInfo);
    
    /**
     * Toplu sigorta durumu güncelleme
     */
    void updatePatientsInsuranceStatus(List<String> patientIds, boolean hasInsurance);

    // === VALIDATION METHODS ===
    
    /**
     * Hasta profil validasyonu
     */
    boolean validatePatientProfile(Patient patient);
    
    /**
     * Sağlık bilgisi validasyonu
     */
    boolean validateHealthInformation(String allergies, String chronicDiseases);
    
    /**
     * Sigorta bilgisi validasyonu
     */
    boolean validateInsuranceInformation(String insuranceCompany, String policyNumber);
    
    /**
     * TC Kimlik No format kontrolü
     */
    boolean isValidTcKimlikNo(String tcKimlikNo);
    
    /**
     * Kan grubu format kontrolü
     */
    boolean isValidBloodType(String bloodType);

    // === PRIVACY & GDPR ===
    
    /**
     * Hasta verilerini anonimleştir
     */
    Patient anonymizePatientData(String patientId);
    
    /**
     * Hasta verilerini export et (GDPR)
     */
    String exportPatientData(String patientId);
    
    /**
     * Hasta verilerini tamamen sil (GDPR - right to be forgotten)
     */
    void permanentlyDeletePatientData(String patientId, String reason);

    // === INTEGRATION METHODS ===
    
    /**
     * External system'den hasta import et
     */
    Patient importPatientFromExternalSystem(String externalPatientId, String systemName);
    
    /**
     * Hasta verilerini external system'e sync et
     */
    void syncPatientToExternalSystem(String patientId, String systemName);
    
    /**
     * Hasta duplikasyon kontrolü
     */
    List<Patient> findPotentialDuplicates(String tcKimlikNo, String firstName, String lastName);
}