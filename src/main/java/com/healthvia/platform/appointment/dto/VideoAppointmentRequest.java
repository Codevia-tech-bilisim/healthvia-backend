package com.healthvia.platform.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Request DTO for creating a video appointment with Zoom meeting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAppointmentRequest {

    @NotBlank(message = "Hasta ID gereklidir")
    private String patientId;

    @NotBlank(message = "Doktor ID gereklidir")
    private String doctorId;

    @NotNull(message = "Randevu tarihi gereklidir")
    @Future(message = "Randevu tarihi gelecekte olmalıdır")
    private LocalDate appointmentDate;

    @NotNull(message = "Başlangıç saati gereklidir")
    private LocalTime startTime;

    @Min(value = 15, message = "Randevu süresi en az 15 dakika olmalıdır")
    @Max(value = 120, message = "Randevu süresi en fazla 120 dakika olabilir")
    @Builder.Default
    private Integer durationMinutes = 30;

    private String treatmentTypeId;

    @Size(max = 1000, message = "Şikayet en fazla 1000 karakter olabilir")
    private String chiefComplaint;

    /**
     * Custom meeting topic (optional, defaults to "Video Konsültasyon - Dr. X")
     */
    private String meetingTopic;

    /**
     * Meeting agenda/notes (optional)
     */
    private String meetingAgenda;

    /**
     * Enable waiting room (default: true)
     */
    @Builder.Default
    private Boolean waitingRoom = true;

    /**
     * Whether this is a follow-up appointment
     */
    @Builder.Default
    private Boolean isFollowUp = false;

    /**
     * Original appointment ID if this is a follow-up
     */
    private String originalAppointmentId;
}
