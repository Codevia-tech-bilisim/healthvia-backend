package com.healthvia.platform.appointment.dto;

import com.healthvia.platform.appointment.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for video appointment with meeting details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAppointmentResponse {

    // Appointment info
    private String id;
    private String patientId;
    private String doctorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private Appointment.AppointmentStatus status;
    private Appointment.ConsultationType consultationType;
    private String chiefComplaint;
    private BigDecimal consultationFee;
    private String currency;
    private LocalDateTime createdAt;

    // Meeting info
    private MeetingDetails meetingDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingDetails {
        private String meetingId;
        private String joinUrl;       // For patients
        private String startUrl;      // For doctors (host)
        private String password;
        private String provider;
    }

    /**
     * Create response from Appointment entity
     */
    public static VideoAppointmentResponse fromAppointment(Appointment appointment) {
        VideoAppointmentResponseBuilder builder = VideoAppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .durationMinutes(appointment.getDurationMinutes())
                .status(appointment.getStatus())
                .consultationType(appointment.getConsultationType())
                .chiefComplaint(appointment.getChiefComplaint())
                .consultationFee(appointment.getConsultationFee())
                .currency(appointment.getCurrency())
                .createdAt(appointment.getCreatedAt());

        if (appointment.getMeetingInfo() != null) {
            builder.meetingDetails(MeetingDetails.builder()
                    .meetingId(appointment.getMeetingInfo().getMeetingId())
                    .joinUrl(appointment.getMeetingInfo().getMeetingUrl())
                    .password(appointment.getMeetingInfo().getMeetingPassword())
                    .provider(appointment.getMeetingInfo().getProvider())
                    .build());
        }

        return builder.build();
    }

    /**
     * Create response with host URL for doctors
     */
    public static VideoAppointmentResponse fromAppointmentForDoctor(
            Appointment appointment, String startUrl) {

        VideoAppointmentResponse response = fromAppointment(appointment);

        if (response.getMeetingDetails() != null) {
            response.getMeetingDetails().setStartUrl(startUrl);
        }

        return response;
    }
}
