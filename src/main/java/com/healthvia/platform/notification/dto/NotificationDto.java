package com.healthvia.platform.notification.dto;

import java.time.LocalDateTime;

import com.healthvia.platform.notification.entity.Notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;
    private String agentId;
    private String kind;
    private String title;
    private String message;
    private boolean read;
    private String actionUrl;
    private LocalDateTime createdAt;

    public static NotificationDto fromEntity(Notification n) {
        if (n == null) return null;
        return NotificationDto.builder()
                .id(n.getId())
                .agentId(n.getRecipientId())
                .kind(n.getKind() != null ? n.getKind().name() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .read(Boolean.TRUE.equals(n.getRead()))
                .actionUrl(n.getActionUrl())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
