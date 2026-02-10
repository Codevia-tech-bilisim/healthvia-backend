// lead/service/LeadService.java
package com.healthvia.platform.lead.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.LeadPriority;
import com.healthvia.platform.lead.entity.Lead.LeadSource;
import com.healthvia.platform.lead.entity.Lead.LeadStatus;

public interface LeadService {

    // === CRUD ===
    Lead create(Lead lead);
    Lead update(String id, Lead lead);
    Optional<Lead> findById(String id);
    void delete(String id, String deletedBy);
    Page<Lead> findAll(Pageable pageable);

    // === DURUM YÖNETİMİ ===
    Lead changeStatus(String id, LeadStatus newStatus, String changedBy, String reason);
    Lead markAsConverted(String id, String patientId, java.math.BigDecimal conversionValue, String changedBy);
    Lead markAsLost(String id, String lostReason, String changedBy);
    Lead markAsSpam(String id, String changedBy);

    // === ATAMA ===
    Lead assignToAgent(String leadId, String agentId, Lead.AssignmentMethod method);
    Lead autoAssign(String leadId);
    Lead transferToAgent(String leadId, String newAgentId, String transferredBy);
    List<Lead> findUnassigned();

    // === SORGULAR ===
    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);
    Page<Lead> findByAgent(String agentId, Pageable pageable);
    Page<Lead> findBySource(LeadSource source, Pageable pageable);
    Page<Lead> findByTags(List<String> tags, Pageable pageable);
    Page<Lead> search(String keyword, Pageable pageable);
    List<Lead> findByLanguageAndStatus(String language, LeadStatus status);

    // === TAKİP ===
    Lead scheduleFollowUp(String id, LocalDateTime followUpAt);
    List<Lead> findLeadsNeedingFollowUp();
    List<Lead> findStaleLeads(int inactiveDays);

    // === TEKRAR EDEN HASTA ===
    Lead checkReturningPatient(String leadId);
    Lead linkToPatient(String leadId, String patientId);

    // === ETİKET ===
    Lead addTag(String id, String tag);
    Lead removeTag(String id, String tag);

    // === İLK YANIT ===
    Lead recordFirstResponse(String id);

    // === İSTATİSTİK ===
    long countAll();
    long countByStatus(LeadStatus status);
    long countBySource(LeadSource source);
    long countByAgent(String agentId);
    long countConvertedByAgent(String agentId);
    long countByDateRange(LocalDateTime start, LocalDateTime end);
}
