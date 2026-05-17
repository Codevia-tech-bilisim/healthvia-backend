package com.healthvia.platform.notification.service;

import java.util.List;

import com.healthvia.platform.notification.entity.Notification;
import com.healthvia.platform.notification.entity.Notification.NotificationKind;

public interface NotificationService {

    Notification create(String recipientId, NotificationKind kind,
                         String title, String message, String actionUrl);

    List<Notification> listForRecipient(String recipientId, int limit);

    long unreadCount(String recipientId);

    void markRead(String id);

    void markAllRead(String recipientId);
}
