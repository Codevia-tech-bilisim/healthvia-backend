package com.healthvia.platform.patientcase.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.entity.PatientCase.CaseStatus;

@Repository
public interface PatientCaseRepository extends MongoRepository<PatientCase, String> {

    Optional<PatientCase> findByCaseNumberAndDeletedFalse(String caseNumber);

    Optional<PatientCase> findByLeadIdAndDeletedFalse(String leadId);

    List<PatientCase> findByPatientIdAndDeletedFalse(String patientId);

    Page<PatientCase> findByStatusAndDeletedFalse(CaseStatus status, Pageable pageable);

    Page<PatientCase> findByAssignedAgentIdAndDeletedFalse(String agentId, Pageable pageable);

    Page<PatientCase> findByStatusInAndDeletedFalse(List<CaseStatus> statuses, Pageable pageable);

    @Query("{ $or: [ " +
        "{'caseNumber': {$regex: ?0, $options: 'i'}}, " +
        "{'treatmentTypeId': {$regex: ?0, $options: 'i'}} " +
        "], 'deleted': false }")
    Page<PatientCase> searchByKeyword(String keyword, Pageable pageable);

    long countByStatusAndDeletedFalse(CaseStatus status);

    long countByAssignedAgentIdAndDeletedFalse(String agentId);
}
