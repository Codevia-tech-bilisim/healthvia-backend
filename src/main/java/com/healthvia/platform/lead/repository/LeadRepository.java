// lead/repository/LeadRepository.java
package com.healthvia.platform.lead.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.LeadPriority;
import com.healthvia.platform.lead.entity.Lead.LeadSource;
import com.healthvia.platform.lead.entity.Lead.LeadStatus;

@Repository
public interface LeadRepository extends MongoRepository<Lead, String> {

    // === TEMEL SORGULAR ===

    Optional<Lead> findByEmailAndDeletedFalse(String email);

    Optional<Lead> findByPhoneAndDeletedFalse(String phone);

    Optional<Lead> findByWhatsappNumberAndDeletedFalse(String whatsappNumber);

    // === DURUM BAZLI ===

    Page<Lead> findByStatusAndDeletedFalse(LeadStatus status, Pageable pageable);

    List<Lead> findByStatusAndDeletedFalseOrderByCreatedAtDesc(LeadStatus status);

    List<Lead> findByStatusInAndDeletedFalse(List<LeadStatus> statuses);

    long countByStatusAndDeletedFalse(LeadStatus status);

    // === AGENT BAZLI ===

    Page<Lead> findByAssignedAgentIdAndDeletedFalse(String agentId, Pageable pageable);

    List<Lead> findByAssignedAgentIdAndStatusAndDeletedFalse(String agentId, LeadStatus status);

    long countByAssignedAgentIdAndDeletedFalse(String agentId);

    long countByAssignedAgentIdAndStatusAndDeletedFalse(String agentId, LeadStatus status);

    // Atanmamış leadler
    List<Lead> findByAssignedAgentIdIsNullAndStatusAndDeletedFalseOrderByCreatedAtAsc(LeadStatus status);

    // === KAYNAK BAZLI ===

    Page<Lead> findBySourceAndDeletedFalse(LeadSource source, Pageable pageable);

    long countBySourceAndDeletedFalse(LeadSource source);

    // === DİL BAZLI ===

    List<Lead> findByLanguageAndStatusAndDeletedFalse(String language, LeadStatus status);

    long countByLanguageAndDeletedFalse(String language);

    // === ÖNCELİK BAZLI ===

    List<Lead> findByPriorityAndStatusNotInAndDeletedFalseOrderByCreatedAtAsc(
            LeadPriority priority, List<LeadStatus> excludeStatuses);

    // === ETİKET BAZLI ===

    @Query("{ 'tags': ?0, 'deleted': false }")
    List<Lead> findByTag(String tag);

    @Query("{ 'tags': { $in: ?0 }, 'deleted': false }")
    Page<Lead> findByTagsIn(List<String> tags, Pageable pageable);

    // === TAKİP ===

    @Query("{ 'nextFollowUpAt': { $lte: ?0 }, 'status': { $nin: ['CONVERTED','LOST','SPAM','ARCHIVED'] }, 'deleted': false }")
    List<Lead> findLeadsNeedingFollowUp(LocalDateTime now);

    @Query("{ 'lastContactAt': { $lte: ?0 }, 'status': { $nin: ['CONVERTED','LOST','SPAM','ARCHIVED','NEW'] }, 'deleted': false }")
    List<Lead> findStaleLeads(LocalDateTime threshold);

    // === TEKRAR EDEN HASTA ===

    List<Lead> findByIsReturningTrueAndDeletedFalse();

    Optional<Lead> findByExistingPatientIdAndDeletedFalse(String patientId);

    // === TEDAVİ BAZLI ===

    @Query("{ 'treatmentTypeId': ?0, 'deleted': false }")
    List<Lead> findByTreatmentTypeId(String treatmentTypeId);

    @Query("{ 'interestedTreatment': { $regex: ?0, $options: 'i' }, 'deleted': false }")
    List<Lead> findByInterestedTreatmentContaining(String treatment);

    // === ARAMA ===

    @Query("{ $or: [ " +
           "{'firstName': {$regex: ?0, $options: 'i'}}, " +
           "{'lastName': {$regex: ?0, $options: 'i'}}, " +
           "{'email': {$regex: ?0, $options: 'i'}}, " +
           "{'phone': {$regex: ?0, $options: 'i'}}, " +
           "{'whatsappNumber': {$regex: ?0, $options: 'i'}}, " +
           "{'instagramHandle': {$regex: ?0, $options: 'i'}}, " +
           "{'notes': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<Lead> search(String keyword, Pageable pageable);

    // === ZAMAN BAZLI ===

    Page<Lead> findByCreatedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByCreatedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);

    // === DÖNÜŞÜM ===

    List<Lead> findByConvertedPatientIdIsNotNullAndDeletedFalse();

    long countByConvertedPatientIdIsNotNullAndDeletedFalse();

    // === İSTATİSTİK ===

    long countByDeletedFalse();

    @Query(value = "{ 'assignedAgentId': ?0, 'status': 'CONVERTED', 'deleted': false }", count = true)
    long countConvertedByAgent(String agentId);
}
