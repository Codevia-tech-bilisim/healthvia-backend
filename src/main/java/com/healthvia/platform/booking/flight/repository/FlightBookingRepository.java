package com.healthvia.platform.booking.flight.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.booking.flight.entity.FlightBooking;

@Repository
public interface FlightBookingRepository extends MongoRepository<FlightBooking, String> {
    List<FlightBooking> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(String caseId);
    List<FlightBooking> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(String patientId);
}
