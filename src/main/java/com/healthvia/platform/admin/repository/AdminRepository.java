// user/repository/AdminRepository.java
package com.healthvia.platform.admin.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.admin.entity.Admin;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {

    // === BASIC ADMIN QUERIES ===
    
    /**
     * Çalışan ID ile admin bulma
     */
    Optional<Admin> findByEmployeeIdAndDeletedFalse(String employeeId);
    
    /**
     * Departman göre adminler
     */
    List<Admin> findByDepartmentAndDeletedFalse(String department);
    
    /**
     * İş unvanı göre adminler
     */
    List<Admin> findByJobTitleContainingIgnoreCaseAndDeletedFalse(String jobTitle);

    // === ADMIN LEVEL QUERIES ===
    
    /**
     * Admin seviyesi göre adminler
     */
    List<Admin> findByAdminLevelAndDeletedFalse(Admin.AdminLevel adminLevel);
    
    /**
     * Süper adminler
     */
    @Query("{ 'adminLevel': 'SUPER_ADMIN', 'deleted': false }")
    List<Admin> findSuperAdmins();
    
    /**
     * Standart adminler
     */
    @Query("{ 'adminLevel': 'STANDARD', 'deleted': false }")
    List<Admin> findStandardAdmins();
    
    /**
     * Yönetici seviyesi adminler
     */
    @Query("{ 'adminLevel': { $in: ['MANAGER', 'SUPER_ADMIN'] }, 'deleted': false }")
    List<Admin> findManagerLevelAdmins();

    // === PERMISSION BASED QUERIES ===
    
    /**
     * Belirli yetkiye sahip adminler
     */
    @Query("{ 'permissions': { $in: [?0] }, 'deleted': false }")
    List<Admin> findAdminsWithPermission(Admin.AdminPermission permission);
    
    /**
     * Kullanıcı yönetimi yetkisi olan adminler
     */
    @Query("{ 'canManageUsers': true, 'deleted': false }")
    List<Admin> findUserManagementAdmins();
    
    /**
     * Doktor yönetimi yetkisi olan adminler
     */
    @Query("{ 'canManageDoctors': true, 'deleted': false }")
    List<Admin> findDoctorManagementAdmins();
    
    /**
     * Klinik yönetimi yetkisi olan adminler
     */
    @Query("{ 'canManageClinics': true, 'deleted': false }")
    List<Admin> findClinicManagementAdmins();
    
    /**
     * Sistem yönetimi yetkisi olan adminler
     */
    @Query("{ 'canManageSystem': true, 'deleted': false }")
    List<Admin> findSystemManagementAdmins();
    
    /**
     * Rapor görüntüleme yetkisi olan adminler
     */
    @Query("{ 'canViewReports': true, 'deleted': false }")
    List<Admin> findReportViewingAdmins();

    // === HIERARCHY QUERIES ===
    
    /**
     * Belirli yöneticinin astları
     */
    List<Admin> findBySupervisorIdAndDeletedFalse(String supervisorId);
    
    /**
     * Yöneticisi olmayan adminler (üst düzey)
     */
    @Query("{ 'supervisorId': null, 'deleted': false }")
    List<Admin> findTopLevelAdmins();
    
    /**
     * Belirli adminin tüm astları (recursive için)
     */
    @Query("{ 'supervisorId': ?0, 'deleted': false }")
    List<Admin> findDirectSubordinates(String supervisorId);

    // === ACTIVITY BASED QUERIES ===
    
    /**
     * Son admin işlemi belirli tarihten sonra olan adminler
     */
    @Query("{ 'lastAdminAction': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findActiveAdminsSince(LocalDateTime lastActionAfter);
    
    /**
     * Belirli süre admin işlemi yapmayan adminler
     */
    @Query("{ 'lastAdminAction': { $lt: ?0 }, 'deleted': false }")
    List<Admin> findInactiveAdminsSince(LocalDateTime lastActionBefore);
    
    /**
     * Hiç admin işlemi yapmamış adminler
     */
    @Query("{ 'lastAdminAction': null, 'deleted': false }")
    List<Admin> findAdminsWithoutActivity();
    
    /**
     * En aktif adminler (çok işlem yapan)
     */
    @Query("{ 'totalActionsPerformed': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findMostActiveAdmins(Integer minActions);

    // === PERFORMANCE QUERIES ===
    
    /**
     * En çok kullanıcı yöneten adminler
     */
    @Query("{ 'usersManaged': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findAdminsWithMostUsers(Integer minUsers);
    
    /**
     * En çok doktor onaylayan adminler
     */
    @Query("{ 'doctorsApproved': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findAdminsWithMostDoctorApprovals(Integer minApprovals);
    
    /**
     * En çok klinik onaylayan adminler
     */
    @Query("{ 'clinicsApproved': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findAdminsWithMostClinicApprovals(Integer minApprovals);
    
    /**
     * Performans skoruna göre adminler
     */
    @Query("{ $expr: { $add: ['$usersManaged', '$doctorsApproved', '$clinicsApproved'] }, $gte: ?0, 'deleted': false }")
    List<Admin> findHighPerformingAdmins(Integer minTotalActions);

    // === EMPLOYMENT QUERIES ===
    
    /**
     * İşe alım tarihi aralığında adminler
     */
    @Query("{ 'hireDate': { $gte: ?0, $lte: ?1 }, 'deleted': false }")
    List<Admin> findAdminsHiredBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Belirli tarihten sonra işe alınan adminler
     */
    @Query("{ 'hireDate': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findAdminsHiredAfter(LocalDateTime hireDate);
    
    /**
     * Kıdem sırasına göre adminler
     */
    @Query("{ 'deleted': false }")
    List<Admin> findAllOrderByHireDateAsc();

    List<Admin> findByIsAvailableTrueAndDeletedFalse();

    @Query("{ 'isAvailable': true, 'spokenLanguages': ?0, 'deleted': false }")
    List<Admin> findAvailableAgentsByLanguage(String language);

    @Query("{ 'isAvailable': true, 'specializations': ?0, 'deleted': false }")
    List<Admin> findAvailableAgentsBySpecialization(String specialization);

    // === SEARCH QUERIES ===
    
    /**
     * Admin arama - isim, departman, unvan
     */
    @Query("{ $or: [ " +
           "{'firstName': {$regex: ?0, $options: 'i'}}, " +
           "{'lastName': {$regex: ?0, $options: 'i'}}, " +
           "{'department': {$regex: ?0, $options: 'i'}}, " +
           "{'jobTitle': {$regex: ?0, $options: 'i'}}, " +
           "{'employeeId': ?0} " +
           "], 'deleted': false }")
    Page<Admin> searchAdmins(String searchTerm, Pageable pageable);
    
    /**
     * Departman ve unvana göre arama
     */
    @Query("{ 'department': {$regex: ?0, $options: 'i'}, 'jobTitle': {$regex: ?1, $options: 'i'}, 'deleted': false }")
    List<Admin> findByDepartmentAndJobTitle(String department, String jobTitle);

    // === COUNT QUERIES ===
    
    /**
     * Departman göre admin sayısı
     */
    long countByDepartmentAndDeletedFalse(String department);
    
    /**
     * Admin seviyesi göre sayı
     */
    long countByAdminLevelAndDeletedFalse(Admin.AdminLevel adminLevel);
    
    /**
     * Aktif admin sayısı (son 30 gün içinde işlem yapan)
     */
    @Query("{ 'lastAdminAction': { $gte: ?0 }, 'deleted': false }")
    long countActiveAdmins(LocalDateTime thirtyDaysAgo);
    
    /**
     * Belirli yetkiye sahip admin sayısı
     */
    @Query("{ 'permissions': { $in: [?0] }, 'deleted': false }")
    long countAdminsWithPermission(Admin.AdminPermission permission);

    // === PERMISSION ANALYSIS QUERIES ===
    
    /**
     * Tüm yetkilere sahip adminler (süper yetkili)
     */
    @Query("{ 'canManageUsers': true, 'canManageDoctors': true, 'canManageClinics': true, 'canManageSystem': true, 'deleted': false }")
    List<Admin> findFullAccessAdmins();
    
    /**
     * Kısıtlı yetkili adminler
     */
    @Query("{ $and: [ " +
           "{ $or: [ {'canManageUsers': false}, {'canManageUsers': null} ] }, " +
           "{ $or: [ {'canManageDoctors': false}, {'canManageDoctors': null} ] }, " +
           "{ $or: [ {'canManageClinics': false}, {'canManageClinics': null} ] }, " +
           "{ $or: [ {'canManageSystem': false}, {'canManageSystem': null} ] } " +
           "], 'deleted': false }")
    List<Admin> findLimitedAccessAdmins();

    // === EXISTENCE CHECKS ===
    
    /**
     * Çalışan ID varlık kontrolü
     */
    boolean existsByEmployeeIdAndDeletedFalse(String employeeId);
    
    /**
     * Departmanda admin varlık kontrolü
     */
    boolean existsByDepartmentAndDeletedFalse(String department);

    // === ANALYTICS QUERIES ===
    
    /**
     * Departman bazlı admin dağılımı
     */
    @Query("{ 'deleted': false }")
    List<Admin> findAllForDepartmentAnalytics();
    
    /**
     * En verimli adminler (toplam işlem / gün sayısı)
     */
    @Query("{ 'totalActionsPerformed': { $gt: 0 }, 'hireDate': { $ne: null }, 'deleted': false }")
    List<Admin> findAdminsForProductivityAnalysis();
    
    /**
     * Yetki dağılım analizi için
     */
    @Query("{ 'permissions': { $exists: true, $not: {$size: 0} }, 'deleted': false }")
    List<Admin> findAdminsWithPermissions();

    // === SECURITY QUERIES ===
    
    /**
     * Sistem seviyesi yetkilere sahip adminler
     */
    @Query("{ $or: [ " +
           "{'permissions': { $in: ['MANAGE_SYSTEM_SETTINGS', 'ACCESS_AUDIT_LOGS', 'SYSTEM_MAINTENANCE'] }}, " +
           "{'canManageSystem': true} " +
           "], 'deleted': false }")
    List<Admin> findSystemLevelAdmins();
    
    /**
     * Sadece görüntüleme yetkisi olan adminler
     */
    @Query("{ 'canViewReports': true, 'canManageUsers': { $ne: true }, 'canManageDoctors': { $ne: true }, 'canManageClinics': { $ne: true }, 'canManageSystem': { $ne: true }, 'deleted': false }")
    List<Admin> findViewOnlyAdmins();

    // === EMERGENCY ACCESS QUERIES ===
    
    /**
     * Acil durum yetkisi olan adminler
     */
    @Query("{ 'permissions': { $in: ['EMERGENCY_ACCESS'] }, 'deleted': false }")
    List<Admin> findEmergencyAccessAdmins();
    
    /**
     * 7/24 erişilebilir adminler (son 24 saat içinde aktif)
     */
    @Query("{ 'lastAdminAction': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findRecentlyActiveAdmins(LocalDateTime last24Hours);

    // === REPORTING QUERIES ===
    
    /**
     * Admin performans raporu için
     */
    @Query("{ 'hireDate': { $gte: ?0 }, 'deleted': false }")
    List<Admin> findAdminsForPerformanceReport(LocalDateTime fromDate);
    
    /**
     * Departman liderleri (supervisor olanlar)
     */
    @Query("{ '_id': { $in: ?0 }, 'deleted': false }")
    List<Admin> findDepartmentLeaders(List<String> supervisorIds);

    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);


}