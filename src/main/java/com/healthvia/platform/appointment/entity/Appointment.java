package com.healthvia.platform.appointment.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import com.healthvia.platform.common.model.BaseEntity;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "appointments")
@CompoundIndex(def = "{'doctorId': 1, 'appointmentDate': 1, 'startTime': 1}")
@CompoundIndex(def = "{'patientId': 1, 'appointmentDate': -1}")
@CompoundIndex(def = "{'status': 1, 'appointmentDate': 1}")
public class Appointment extends BaseEntity {

    // === TEMEL BİLGİLER ===
    @NotBlank(message = "Hasta ID boş olamaz")
    @Field("patient_id")
    @Indexed
    private String patientId;

    @NotBlank(message = "Doktor ID boş olamaz") 
    @Field("doctor_id")
    @Indexed
    private String doctorId;

    @NotNull(message = "Randevu tarihi boş olamaz")
    @Field("appointment_date")
    @Indexed
    private LocalDate appointmentDate;

    @NotNull(message = "Başlangıç saati boş olamaz")
    @Field("start_time")
    private LocalTime startTime;

    @NotNull(message = "Bitiş saati boş olamaz")
    @Field("end_time")
    private LocalTime endTime;

    @NotNull(message = "Süre belirtilmelidir")
    @Min(value = 15, message = "Randevu süresi en az 15 dakika olmalı")
    @Max(value = 180, message = "Randevu süresi en fazla 180 dakika olabilir")
    @Field("duration_minutes")
    private Integer durationMinutes;

    // === DURUM YÖNETİMİ ===
    @NotNull(message = "Randevu durumu belirtilmelidir")
    @Indexed
    private AppointmentStatus status;

    @Field("previous_status") 
    private AppointmentStatus previousStatus;

    @Field("status_changed_at")
    private LocalDateTime statusChangedAt;

    @Field("status_changed_by")
    private String statusChangedBy;

    @Size(max = 500, message = "Durum değişiklik notu en fazla 500 karakter olabilir")
    @Field("status_change_reason")
    private String statusChangeReason;

    // === RANDEVU TİPLERİ ===
    @NotNull(message = "Konsültasyon tipi belirtilmelidir")
    @Field("consultation_type")
    private ConsultationType consultationType;

    @Field("treatment_type_id")
    private String treatmentTypeId;

    @Size(max = 1000, message = "Şikayet metni en fazla 1000 karakter olabilir")
    @Field("chief_complaint")
    private String chiefComplaint;

    @Field("is_follow_up")
    private Boolean isFollowUp;

    @Field("original_appointment_id")
    private String originalAppointmentId;

    // === ÜCRET BİLGİLERİ ===
    @NotNull(message = "Randevu ücreti belirtilmelidir")
    @DecimalMin(value = "0.0", message = "Ücret negatif olamaz")
    @Field("consultation_fee")
    private BigDecimal consultationFee;

    @Field("currency")
    private String currency = "TRY";

    @Field("payment_status")
    private PaymentStatus paymentStatus;

    @Field("payment_id")
    private String paymentId;

    @DecimalMin(value = "0.0", message = "Hotel price cannot be negative")
    @Field("hotel_price")
    private BigDecimal hotelPrice;

    @DecimalMin(value = "0.0", message = "Flight price cannot be negative")
    @Field("flight_price")
    private BigDecimal flightPrice;

    @DecimalMin(value = "0.0", message = "Total price cannot be negative")
    @Field("total_price")
    private BigDecimal totalPrice;

    // === RANDEVU SÜRECI ===
    @Field("confirmed_at")
    private LocalDateTime confirmedAt;

    @Field("confirmed_by")
    private String confirmedBy;

    @Field("checked_in_at")
    private LocalDateTime checkedInAt;

    @Field("consultation_started_at")
    private LocalDateTime consultationStartedAt;

    @Field("consultation_ended_at")
    private LocalDateTime consultationEndedAt;

    @Field("actual_duration_minutes")
    private Integer actualDurationMinutes;

    // === NOTLAR VE BELGELİR ===
    @Size(max = 2000, message = "Doktor notu en fazla 2000 karakter olabilir")
    @Field("doctor_notes")
    private String doctorNotes;

    @Size(max = 1000, message = "Hasta notu en fazla 1000 karakter olabilir")
    @Field("patient_notes") 
    private String patientNotes;

    @Field("prescription_id")
    private String prescriptionId;

    @Field("medical_documents")
    private List<String> medicalDocuments;

    // === VIDEO KONSÜLTASYON ===
    @Field("meeting_info")
    private MeetingInfo meetingInfo;

    // === OTEL & UÇUŞ REZERVASYON BİLGİLERİ ===
    @Field("hotel_booking_id")
    private String hotelBookingId;

    @Field("hotel_booking_name")
    private String hotelBookingName;

    @Field("flight_booking_id")
    private String flightBookingId;

    @Field("flight_booking_details")
    private String flightBookingDetails;

    // === İPTAL VE ERTELEMELer ===
    @Field("cancelled_at")
    private LocalDateTime cancelledAt;

    @Field("cancelled_by")
    private String cancelledBy;

    @Field("cancellation_reason")
    private String cancellationReason;

    @Field("is_rescheduled")
    private Boolean isRescheduled;

    @Field("rescheduled_from_id")
    private String rescheduledFromId;

    @Field("rescheduled_to_id") 
    private String rescheduledToId;

    // === BİLDİRİM VE HATIRLATMA ===
    @Field("reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Field("confirmation_reminder_sent")
    private Boolean confirmationReminderSent;

    @Field("sms_notifications_enabled")
    private Boolean smsNotificationsEnabled = true;

    @Field("email_notifications_enabled") 
    private Boolean emailNotificationsEnabled = true;

    // === ENUM TANIMLAMLARI ===
    public enum AppointmentStatus {
        PENDING,        // Onay bekliyor
        CONFIRMED,      // Onaylandı
        CHECKED_IN,     // Check-in yapıldı
        IN_PROGRESS,    // Muayene devam ediyor
        COMPLETED,      // Tamamlandı
        CANCELLED,      // İptal edildi
        NO_SHOW,        // Gelmedi
        RESCHEDULED     // Ertelendi
    }

    public enum ConsultationType {
        IN_PERSON,      // Yüz yüze
        VIDEO_CALL,     // Video görüşme
        PHONE_CALL,     // Telefon görüşmesi
        FOLLOW_UP       // Kontrol
    }

    public enum PaymentStatus {
        PENDING,        // Ödeme bekleniyor
        PAID,           // Ödendi
        FAILED,         // Ödeme başarısız
        REFUNDED,       // İade edildi
        PARTIAL_REFUND  // Kısmi iade
    }

    // === İÇ SINIFLAR ===
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingInfo {
        @Field("meeting_id")
        private String meetingId;
        
        @Field("meeting_url")
        private String meetingUrl;
        
        @Field("meeting_password")
        private String meetingPassword;
        
        @Field("provider")
        private String provider;
        
        @Field("started_at")
        private LocalDateTime startedAt;
        
        @Field("ended_at")
        private LocalDateTime endedAt;
        
        @Field("participants")
        private List<String> participants;
    }

    // === HELPER METHODS ===
    public boolean isUpcoming() {
        LocalDateTime appointmentDateTime = appointmentDate.atTime(startTime);
        return appointmentDateTime.isAfter(LocalDateTime.now()) && 
               !status.equals(AppointmentStatus.CANCELLED);
    }

    public boolean isToday() {
        return appointmentDate.equals(LocalDate.now());
    }

   public boolean canBeCancelled() {
    return status.equals(AppointmentStatus.CONFIRMED) ||
           status.equals(AppointmentStatus.CHECKED_IN);
    }

    public boolean canBeRescheduled() {
        return canBeCancelled();
    }

    public boolean requiresPayment() {
        return paymentStatus == null || paymentStatus.equals(PaymentStatus.PENDING);
    }

    public void confirm(String confirmedBy) {
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = confirmedBy;
        this.statusChangedAt = LocalDateTime.now();
        this.statusChangedBy = confirmedBy;
    }

    public void cancel(String cancelledBy, String reason) {
        this.previousStatus = this.status;
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = cancelledBy;
        this.cancellationReason = reason;
        this.statusChangedAt = LocalDateTime.now();
        this.statusChangedBy = cancelledBy;
    }

    public void markAsCompleted(String completedBy) {
        this.status = AppointmentStatus.COMPLETED;
        this.consultationEndedAt = LocalDateTime.now();
        this.statusChangedAt = LocalDateTime.now();
        this.statusChangedBy = completedBy;
        
        if (consultationStartedAt != null) {
            long minutes = java.time.Duration.between(
                consultationStartedAt, consultationEndedAt).toMinutes();
            this.actualDurationMinutes = (int) minutes;
        }
    }
}