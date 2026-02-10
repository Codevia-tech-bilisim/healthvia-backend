// lead/service/impl/LeadServiceImpl.java
package com.healthvia.platform.lead.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.service.AdminService;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.AssignmentMethod;
import com.healthvia.platform.lead.entity.Lead.LeadPriority;
import com.healthvia.platform.lead.entity.Lead.LeadSource;
import com.healthvia.platform.lead.entity.Lead.LeadStatus;
import com.healthvia.platform.lead.repository.LeadRepository;
import com.healthvia.platform.lead.service.LeadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final AdminService adminService;

    // === CRUD ===

    @Override
    public Lead create(Lead lead) {
        log.info("Creating lead from source: {} language: {}", lead.getSource(), lead.getLanguage());

        if (lead.getStatus() == null) {
            lead.setStatus(LeadStatus.NEW);
        }
        if (lead.getPriority() == null) {
            lead.setPriority(LeadPriority.MEDIUM);
        }
        if (lead.getStatusChangedAt() == null) {
            lead.setStatusChangedAt(LocalDateTime.now());
        }

        // Tekrar eden hasta kontrolü
        checkAndMarkReturning(lead);

        Lead saved = leadRepository.save(lead);
        log.info("Lead created with id: {}", saved.getId());
        return saved;
    }

    @Override
    public Lead update(String id, Lead updated) {
        Lead existing = findByIdOrThrow(id);

        if (updated.getFirstName() != null) existing.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null) existing.setLastName(updated.getLastName());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getWhatsappNumber() != null) existing.setWhatsappNumber(updated.getWhatsappNumber());
        if (updated.getInstagramHandle() != null) existing.setInstagramHandle(updated.getInstagramHandle());
        if (updated.getLanguage() != null) existing.setLanguage(updated.getLanguage());
        if (updated.getCountry() != null) existing.setCountry(updated.getCountry());
        if (updated.getCity() != null) existing.setCity(updated.getCity());
        if (updated.getInterestedTreatment() != null) existing.setInterestedTreatment(updated.getInterestedTreatment());
        if (updated.getTreatmentTypeId() != null) existing.setTreatmentTypeId(updated.getTreatmentTypeId());
        if (updated.getInterestedTreatments() != null) existing.setInterestedTreatments(updated.getInterestedTreatments());
        if (updated.getBudgetRange() != null) existing.setBudgetRange(updated.getBudgetRange());
        if (updated.getPreferredDates() != null) existing.setPreferredDates(updated.getPreferredDates());
        if (updated.getNotes() != null) existing.setNotes(updated.getNotes());
        if (updated.getPriority() != null) existing.setPriority(updated.getPriority());

        return leadRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lead> findById(String id) {
        return leadRepository.findById(id).filter(l -> !l.isDeleted());
    }

    @Override
    public void delete(String id, String deletedBy) {
        Lead lead = findByIdOrThrow(id);
        lead.markAsDeleted(deletedBy);
        leadRepository.save(lead);
        log.info("Lead {} soft deleted by {}", id, deletedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> findAll(Pageable pageable) {
        return leadRepository.findAll(pageable);
    }

    // === DURUM YÖNETİMİ ===

    @Override
    public Lead changeStatus(String id, LeadStatus newStatus, String changedBy, String reason) {
        Lead lead = findByIdOrThrow(id);
        log.info("Lead {} status: {} → {}", id, lead.getStatus(), newStatus);
        lead.changeStatus(newStatus, changedBy, reason);
        return leadRepository.save(lead);
    }

    @Override
    public Lead markAsConverted(String id, String patientId, java.math.BigDecimal conversionValue, String changedBy) {
        Lead lead = findByIdOrThrow(id);
        lead.changeStatus(LeadStatus.CONVERTED, changedBy, "Hasta kaydı oluşturuldu");
        lead.setConvertedPatientId(patientId);
        lead.setConvertedAt(LocalDateTime.now());
        lead.setConversionValue(conversionValue);

        // Agent istatistiği güncelle
        if (lead.getAssignedAgentId() != null) {
            try {
                adminService.findById(lead.getAssignedAgentId())
                        .ifPresent(admin -> {
                            admin.recordLeadConversion();
                            // adminRepository.save yerine service üzerinden güncelleme yapılabilir
                        });
            } catch (Exception e) {
                log.warn("Agent conversion stat güncellenemedi: {}", e.getMessage());
            }
        }

        log.info("Lead {} converted to patient {}", id, patientId);
        return leadRepository.save(lead);
    }

    @Override
    public Lead markAsLost(String id, String lostReason, String changedBy) {
        Lead lead = findByIdOrThrow(id);
        lead.changeStatus(LeadStatus.LOST, changedBy, lostReason);
        lead.setLostReason(lostReason);
        return leadRepository.save(lead);
    }

    @Override
    public Lead markAsSpam(String id, String changedBy) {
        Lead lead = findByIdOrThrow(id);
        lead.changeStatus(LeadStatus.SPAM, changedBy, "Spam olarak işaretlendi");
        return leadRepository.save(lead);
    }

    // === ATAMA ===

    @Override
    public Lead assignToAgent(String leadId, String agentId, AssignmentMethod method) {
        Lead lead = findByIdOrThrow(leadId);

        // Önceki agent varsa geçmişe ekle
        if (lead.getAssignedAgentId() != null) {
            if (lead.getPreviousAgentIds() == null) {
                lead.setPreviousAgentIds(new ArrayList<>());
            }
            lead.getPreviousAgentIds().add(lead.getAssignedAgentId());
        }

        lead.setAssignedAgentId(agentId);
        lead.setAssignedAt(LocalDateTime.now());
        lead.setAssignmentMethod(method);

        // Agent adını al
        adminService.findById(agentId).ifPresent(admin -> {
            lead.setAssignedAgentName(admin.getFullName());
            admin.recordLeadAssignment();
            admin.incrementActiveChats();
        });

        // Durum NEW ise ASSIGNED'a geç
        if (LeadStatus.NEW.equals(lead.getStatus())) {
            lead.changeStatus(LeadStatus.ASSIGNED, agentId, "Agent atandı");
        }

        log.info("Lead {} assigned to agent {} via {}", leadId, agentId, method);
        return leadRepository.save(lead);
    }

    @Override
    public Lead autoAssign(String leadId) {
        Lead lead = findByIdOrThrow(leadId);

        // Dil ve tedavi alanına göre en uygun agent'ı bul
        Admin bestAgent = adminService.findBestAvailableAgent(
                lead.getLanguage(),
                lead.getInterestedTreatment()
        );

        if (bestAgent == null) {
            // Sadece dil bazlı dene
            bestAgent = adminService.findBestAvailableAgent(lead.getLanguage(), null);
        }

        if (bestAgent == null) {
            // Herhangi bir müsait agent
            bestAgent = adminService.findBestAvailableAgent(null, null);
        }

        if (bestAgent != null) {
            return assignToAgent(leadId, bestAgent.getId(), AssignmentMethod.AUTO);
        }

        log.warn("No available agent for lead {}", leadId);
        return lead;
    }

    @Override
    public Lead transferToAgent(String leadId, String newAgentId, String transferredBy) {
        log.info("Transferring lead {} to agent {} by {}", leadId, newAgentId, transferredBy);

        Lead lead = findByIdOrThrow(leadId);

        // Eski agent'ın active chat'ini düşür
        if (lead.getAssignedAgentId() != null) {
            adminService.findById(lead.getAssignedAgentId())
                    .ifPresent(Admin::decrementActiveChats);
        }

        return assignToAgent(leadId, newAgentId, AssignmentMethod.MANUAL);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> findUnassigned() {
        return leadRepository.findByAssignedAgentIdIsNullAndStatusAndDeletedFalseOrderByCreatedAtAsc(LeadStatus.NEW);
    }

    // === SORGULAR ===

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> findByStatus(LeadStatus status, Pageable pageable) {
        return leadRepository.findByStatusAndDeletedFalse(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> findByAgent(String agentId, Pageable pageable) {
        return leadRepository.findByAssignedAgentIdAndDeletedFalse(agentId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> findBySource(LeadSource source, Pageable pageable) {
        return leadRepository.findBySourceAndDeletedFalse(source, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> findByTags(List<String> tags, Pageable pageable) {
        return leadRepository.findByTagsIn(tags, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> search(String keyword, Pageable pageable) {
        return leadRepository.search(keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> findByLanguageAndStatus(String language, LeadStatus status) {
        return leadRepository.findByLanguageAndStatusAndDeletedFalse(language, status);
    }

    // === TAKİP ===

    @Override
    public Lead scheduleFollowUp(String id, LocalDateTime followUpAt) {
        Lead lead = findByIdOrThrow(id);
        lead.setNextFollowUpAt(followUpAt);
        lead.setFollowUpCount(lead.getFollowUpCount() + 1);

        if (!LeadStatus.FOLLOW_UP.equals(lead.getStatus())) {
            lead.changeStatus(LeadStatus.FOLLOW_UP, null, "Takip planlandı");
        }

        return leadRepository.save(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> findLeadsNeedingFollowUp() {
        return leadRepository.findLeadsNeedingFollowUp(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> findStaleLeads(int inactiveDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(inactiveDays);
        return leadRepository.findStaleLeads(threshold);
    }

    // === TEKRAR EDEN HASTA ===

    @Override
    public Lead checkReturningPatient(String leadId) {
        Lead lead = findByIdOrThrow(leadId);
        checkAndMarkReturning(lead);
        return leadRepository.save(lead);
    }

    @Override
    public Lead linkToPatient(String leadId, String patientId) {
        Lead lead = findByIdOrThrow(leadId);
        lead.setExistingPatientId(patientId);
        lead.setIsReturning(true);
        return leadRepository.save(lead);
    }

    // === ETİKET ===

    @Override
    public Lead addTag(String id, String tag) {
        Lead lead = findByIdOrThrow(id);
        if (lead.getTags() == null) {
            lead.setTags(new HashSet<>());
        }
        lead.getTags().add(tag.toUpperCase());
        return leadRepository.save(lead);
    }

    @Override
    public Lead removeTag(String id, String tag) {
        Lead lead = findByIdOrThrow(id);
        if (lead.getTags() != null) {
            lead.getTags().remove(tag.toUpperCase());
        }
        return leadRepository.save(lead);
    }

    // === İLK YANIT ===

    @Override
    public Lead recordFirstResponse(String id) {
        Lead lead = findByIdOrThrow(id);
        if (lead.getFirstResponseAt() == null) {
            lead.setFirstResponseAt(LocalDateTime.now());
            long responseSeconds = java.time.Duration.between(
                    lead.getCreatedAt(), lead.getFirstResponseAt()
            ).getSeconds();
            lead.setFirstResponseTimeSeconds(responseSeconds);
            log.info("Lead {} first response time: {} seconds", id, responseSeconds);
        }
        lead.setLastContactAt(LocalDateTime.now());
        return leadRepository.save(lead);
    }

    // === İSTATİSTİK ===

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return leadRepository.countByDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(LeadStatus status) {
        return leadRepository.countByStatusAndDeletedFalse(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySource(LeadSource source) {
        return leadRepository.countBySourceAndDeletedFalse(source);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAgent(String agentId) {
        return leadRepository.countByAssignedAgentIdAndDeletedFalse(agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countConvertedByAgent(String agentId) {
        return leadRepository.countConvertedByAgent(agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDateRange(LocalDateTime start, LocalDateTime end) {
        return leadRepository.countByCreatedAtBetweenAndDeletedFalse(start, end);
    }

    // === PRIVATE HELPERS ===

    private Lead findByIdOrThrow(String id) {
        return leadRepository.findById(id)
                .filter(l -> !l.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));
    }

    private void checkAndMarkReturning(Lead lead) {
        // Email ile kontrol
        if (lead.getEmail() != null) {
            leadRepository.findByEmailAndDeletedFalse(lead.getEmail())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(lead.getId())) {
                            lead.setIsReturning(true);
                            if (existing.getConvertedPatientId() != null) {
                                lead.setExistingPatientId(existing.getConvertedPatientId());
                            }
                        }
                    });
        }

        // Telefon ile kontrol
        if (!lead.getIsReturning() && lead.getPhone() != null) {
            leadRepository.findByPhoneAndDeletedFalse(lead.getPhone())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(lead.getId())) {
                            lead.setIsReturning(true);
                            if (existing.getConvertedPatientId() != null) {
                                lead.setExistingPatientId(existing.getConvertedPatientId());
                            }
                        }
                    });
        }
    }
}
