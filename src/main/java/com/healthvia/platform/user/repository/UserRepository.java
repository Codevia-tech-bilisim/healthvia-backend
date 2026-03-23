// user/repository/UserRepository.java
package com.healthvia.platform.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.common.enums.Language;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.common.enums.UserStatus;
import com.healthvia.platform.user.entity.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // === BASIC FINDERS ===
    
    /**
     * Email ile kullanıcı bulma - Authentication için kritik
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Email ile aktif kullanıcı bulma
     */
    Optional<User> findByEmailAndDeletedFalse(String email);
    
    /**
     * Telefon ile kullanıcı bulma
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * Email veya telefon ile kullanıcı bulma
     */
    @Query("{ $or: [ {'email': ?0}, {'phone': ?0} ] }")
    Optional<User> findByEmailOrPhone(String emailOrPhone);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    // === ROLE BASED QUERIES ===
    
    /**
     * Role göre kullanıcı listesi
     */
    List<User> findByRole(UserRole role);
    
    /**
     * Role göre aktif kullanıcı listesi  
     */
    List<User> findByRoleAndDeletedFalse(UserRole role);
    
    /**
     * Role ve status göre kullanıcı listesi (sayfalı)
     */
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    // === STATUS BASED QUERIES ===
    
    /**
     * Status göre kullanıcı listesi
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * Email doğrulanmamış kullanıcılar
     */
    List<User> findByEmailVerifiedFalseAndDeletedFalse();
    
    /**
     * Telefon doğrulanmamış kullanıcılar
     */
    List<User> findByPhoneVerifiedFalseAndDeletedFalse();
    
    /**
     * Hesabı kilitli kullanıcılar
     */
    @Query("{'accountLockedUntil': {$gt: ?0}}")
    List<User> findLockedUsers(LocalDateTime currentTime);

    // === SEARCH & FILTER QUERIES ===
    
    /**
     * İsim veya soyisim ile arama
     */
    @Query("{ $or: [ " +
           "{'firstName': {$regex: ?0, $options: 'i'}}, " +
           "{'lastName': {$regex: ?0, $options: 'i'}}, " +
           "{'email': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<User> searchUsers(String searchTerm, Pageable pageable);
    
    /**
     * Role ve arama terimi ile
     */
    @Query("{ 'role': ?0, " +
           "$or: [ " +
           "{'firstName': {$regex: ?1, $options: 'i'}}, " +
           "{'lastName': {$regex: ?1, $options: 'i'}}, " +
           "{'email': {$regex: ?1, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<User> searchUsersByRole(UserRole role, String searchTerm, Pageable pageable);
    
    /**
     * İl ve ilçe göre kullanıcılar
     */
    List<User> findByProvinceAndDistrictAndDeletedFalse(String province, String district);
    
    /**
     * Dil tercihi göre kullanıcılar
     */
    List<User> findByPreferredLanguageAndDeletedFalse(Language language);

    // === EXISTENCE CHECKS ===
    
    /**
     * Email varlık kontrolü
     */
    boolean existsByEmail(String email);
    
    /**
     * Telefon varlık kontrolü
     */
    boolean existsByPhone(String phone);
    
    /**
     * Email varlık kontrolü (deleted hariç)
     */
    boolean existsByEmailAndDeletedFalse(String email);
    
    /**
     * Telefon varlık kontrolü (deleted hariç)
     */
    boolean existsByPhoneAndDeletedFalse(String phone);

    // === COUNT QUERIES ===
    
    /**
     * Role göre kullanıcı sayısı
     */
    long countByRole(UserRole role);
    
    /**
     * Status göre kullanıcı sayısı
     */
    long countByStatus(UserStatus status);
    
    /**
     * Role ve status göre kullanıcı sayısı
     */
    long countByRoleAndStatus(UserRole role, UserStatus status);
    
    /**
     * Email doğrulanmamış kullanıcı sayısı
     */
    long countByEmailVerifiedFalseAndDeletedFalse();
    
    /**
     * Bu ay kayıt olan kullanıcı sayısı
     */
    @Query("{ 'createdAt': {$gte: ?0, $lt: ?1}, 'deleted': false }")
    long countUsersRegisteredBetween(LocalDateTime startDate, LocalDateTime endDate);

    // === DATE RANGE QUERIES ===
    
    /**
     * Belirli tarih aralığında kayıt olan kullanıcılar
     */
    @Query("{ 'createdAt': {$gte: ?0, $lt: ?1}, 'deleted': false }")
    List<User> findUsersRegisteredBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Son giriş tarihi belirli bir tarihten eski olan kullanıcılar
     */
    @Query("{ 'lastLoginDate': {$lt: ?0}, 'deleted': false }")
    List<User> findInactiveUsersSince(LocalDateTime lastLoginBefore);
    
    /**
     * Belirli tarihten sonra hiç giriş yapmamış kullanıcılar
     */
    @Query("{ 'lastLoginDate': null, 'createdAt': {$lt: ?0}, 'deleted': false }")
    List<User> findNeverLoggedInUsersBefore(LocalDateTime createdBefore);

    // === FAILED LOGIN QUERIES ===
    
    /**
     * Başarısız giriş denemesi fazla olan kullanıcılar
     */
    @Query("{ 'failedLoginAttempts': {$gte: ?0}, 'deleted': false }")
    List<User> findUsersWithFailedAttempts(int minFailedAttempts);

    // === GDPR & CONSENT QUERIES ===
    
    /**
     * GDPR onayı verilmemiş kullanıcılar
     */
    List<User> findByGdprConsentFalseAndDeletedFalse();
    
    /**
     * Veri işleme onayı verilmemiş kullanıcılar
     */
    List<User> findByDataProcessingConsentFalseAndDeletedFalse();
    
    /**
     * Marketing onayı vermiş kullanıcılar
     */
    List<User> findByMarketingConsentTrueAndDeletedFalse();

    // === PROFILE COMPLETION QUERIES ===
    
    /**
     * Profil tamamlanma oranı düşük kullanıcılar
     */
    @Query("{ 'profileCompletionRate': {$lt: ?0}, 'deleted': false }")
    List<User> findUsersWithLowProfileCompletion(int maxCompletionRate);
    
    /**
     * Profil tamamlanma oranına göre sıralı kullanıcılar
     */
    @Query("{ 'deleted': false }")
    List<User> findAllOrderByProfileCompletionRateDesc();

    // === SOFT DELETE QUERIES ===
    
    /**
     * Silinen kullanıcılar (admin için)
     */
    List<User> findByDeletedTrue();
    
    /**
     * Belirli tarihten sonra silinen kullanıcılar
     */
    @Query("{ 'deleted': true, 'deletedAt': {$gte: ?0} }")
    List<User> findDeletedUsersSince(LocalDateTime deletedAfter);

    // === ADMIN QUERIES ===
    
    /**
     * Tüm kullanıcılar (deleted dahil) - Admin için
     */
    @Query("{}")
    Page<User> findAllIncludingDeleted(Pageable pageable);
    
    /**
     * Role göre tüm kullanıcılar (deleted dahil)
     */
    Page<User> findByRole(UserRole role, Pageable pageable);
}