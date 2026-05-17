package com.healthvia.platform.notification.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.healthvia.platform.notification.entity.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByRecipientIdAndDeletedFalseOrderByCreatedAtDesc(
            String recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndReadFalseAndDeletedFalse(String recipientId);

    long countByRecipientIdAndReadFalseAndDeletedFalse(String recipientId);
}
