package com.healthvia.platform.zoom.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a Zoom meeting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoomMeetingRequest {

    /**
     * Meeting topic/title
     */
    @NotBlank(message = "Toplantı konusu gereklidir")
    private String topic;

    /**
     * Meeting start time (UTC)
     */
    @NotNull(message = "Başlangıç zamanı gereklidir")
    @Future(message = "Başlangıç zamanı gelecekte olmalıdır")
    private LocalDateTime startTime;

    /**
     * Meeting duration in minutes (default: 30)
     */
    @Min(value = 15, message = "Toplantı süresi en az 15 dakika olmalıdır")
    @Max(value = 240, message = "Toplantı süresi en fazla 240 dakika olabilir")
    @Builder.Default
    private Integer durationMinutes = 30;

    /**
     * Meeting agenda/description (optional)
     */
    private String agenda;

    /**
     * Patient ID for reference
     */
    private String patientId;

    /**
     * Doctor ID for reference
     */
    private String doctorId;

    /**
     * Appointment ID for reference
     */
    private String appointmentId;

    /**
     * Timezone (default: Europe/Istanbul)
     */
    @Builder.Default
    private String timezone = "Europe/Istanbul";

    /**
     * Enable waiting room (default: true for healthcare privacy)
     */
    @Builder.Default
    private Boolean waitingRoom = true;

    /**
     * Enable meeting password (default: true)
     */
    @Builder.Default
    private Boolean requirePassword = true;

    /**
     * Allow join before host (default: false)
     */
    @Builder.Default
    private Boolean joinBeforeHost = false;

    /**
     * Mute participants on entry (default: true)
     */
    @Builder.Default
    private Boolean muteOnEntry = true;
}
