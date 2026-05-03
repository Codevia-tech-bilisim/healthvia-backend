package com.healthvia.platform.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.payment.entity.PaymentRequest;

@Repository
public interface PaymentRequestRepository extends MongoRepository<PaymentRequest, String> {
    Optional<PaymentRequest> findByLinkTokenAndDeletedFalse(String token);
    List<PaymentRequest> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(String caseId);
    List<PaymentRequest> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(String patientId);
    List<PaymentRequest> findByAgentIdAndDeletedFalseOrderByCreatedAtDesc(String agentId);
    long countByStatusAndDeletedFalse(PaymentRequest.PaymentStatus status);
}
