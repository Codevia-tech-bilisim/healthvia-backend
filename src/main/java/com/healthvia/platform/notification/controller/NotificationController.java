package com.healthvia.platform.notification.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.notification.dto.NotificationDto;
import com.healthvia.platform.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','CEO','AGENT')")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationDto>> list(
            @RequestParam(required = false) String agentId,
            @RequestParam(defaultValue = "50") int limit) {
        List<NotificationDto> items = notificationService.listForRecipient(resolve(agentId), limit)
                .stream().map(NotificationDto::fromEntity).toList();
        return ApiResponse.success(items);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@RequestParam(required = false) String agentId) {
        return ApiResponse.success(notificationService.unreadCount(resolve(agentId)));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String id) {
        notificationService.markRead(id);
        return ApiResponse.success("Bildirim okundu olarak işaretlendi");
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@RequestParam(required = false) String agentId) {
        notificationService.markAllRead(resolve(agentId));
        return ApiResponse.success("Tüm bildirimler okundu olarak işaretlendi");
    }

    /** Use the explicit agentId when given, otherwise the caller's own id. */
    private String resolve(String agentId) {
        return (agentId != null && !agentId.isBlank())
                ? agentId
                : SecurityUtils.getCurrentUserId();
    }
}
