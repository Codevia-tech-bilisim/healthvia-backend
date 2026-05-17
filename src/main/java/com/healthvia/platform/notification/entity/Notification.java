package com.healthvia.platform.notification.entity;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.healthvia.platform.common.model.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * In-app notification for a staff member (agent / admin).
 *
 * Field names are kept identical to the Java property names (no @Field
 * remapping) so derived-query lookups stay trivially consistent.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notifications")
public class Notification extends BaseEntity {

    @NotBlank(message = "Alıcı boş olamaz")
    @Indexed
    private String recipientId;

    @NotNull(message = "Bildirim tipi belirtilmelidir")
    private NotificationKind kind;

    @NotBlank(message = "Başlık boş olamaz")
    private String title;

    private String message;

    /** Optional deep-link inside the dashboard, e.g. "/inbox". */
    private String actionUrl;

    private Boolean read;

    public Boolean getRead() {
        return read != null ? read : false;
    }

    /** Mirrors the agent dashboard's NotificationKind union. */
    public enum NotificationKind {
        NEW_LEAD,
        LEAD_ASSIGNED,
        NEW_MESSAGE,
        APPOINTMENT_REMINDER,
        PAYMENT_RECEIVED,
        SLA_WARNING,
        SYSTEM
    }
}
