package com.healthvia.platform.booking.hotel.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.booking.hotel.entity.HotelBooking;

@Repository
public interface HotelBookingRepository extends MongoRepository<HotelBooking, String> {
    List<HotelBooking> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(String caseId);
    List<HotelBooking> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(String patientId);
}
