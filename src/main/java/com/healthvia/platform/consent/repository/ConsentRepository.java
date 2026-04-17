package com.healthvia.platform.consent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.consent.entity.Consent;

@Repository
public interface ConsentRepository extends MongoRepository<Consent, String> {

    List<Consent> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(String caseId);

    List<Consent> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(String patientId);

    Optional<Consent> findByIdAndDeletedFalse(String id);
}
