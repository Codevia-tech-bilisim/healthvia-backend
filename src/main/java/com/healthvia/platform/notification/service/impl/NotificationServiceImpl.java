package com.healthvia.platform.notification.service.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.notification.entity.Notification;
import com.healthvia.platform.notification.entity.Notification.NotificationKind;
import com.healthvia.platform.notification.repository.NotificationRepository;
import com.healthvia.platform.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_LIMIT = 200;

    private final NotificationRepository repository;

    @Override
    public Notification create(String recipientId, NotificationKind kind,
                               String title, String message, String actionUrl) {
        if (recipientId == null || recipientId.isBlank()) {
            log.debug("Notification skipped — no recipient");
            return null;
        }
        Notification n = Notification.builder()
                .recipientId(recipientId)
                .kind(kind)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .read(false)
                .build();
        return repository.save(n);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> listForRecipient(String recipientId, int limit) {
        int capped = Math.min(Math.max(1, limit), MAX_LIMIT);
        return repository.findByRecipientIdAndDeletedFalseOrderByCreatedAtDesc(
                recipientId, PageRequest.of(0, capped)).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String recipientId) {
        return repository.countByRecipientIdAndReadFalseAndDeletedFalse(recipientId);
    }

    @Override
    public void markRead(String id) {
        repository.findById(id)
                .filter(n -> !n.isDeleted())
                .ifPresent(n -> {
                    n.setRead(true);
                    repository.save(n);
                });
    }

    @Override
    public void markAllRead(String recipientId) {
        List<Notification> unread =
                repository.findByRecipientIdAndReadFalseAndDeletedFalse(recipientId);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
        log.debug("Marked {} notifications read for {}", unread.size(), recipientId);
    }
}
