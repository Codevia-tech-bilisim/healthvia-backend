package com.healthvia.platform.appointment.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Set;

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
@Document(collection = "time_slots")
@CompoundIndex(def = "{'doctorId': 1, 'date': 1, 'startTime': 1}")
@CompoundIndex(def = "{'doctorId': 1, 'status': 1, 'date': 1}")
public class TimeSlot extends BaseEntity {

    @NotBlank(message = "Doktor ID boş olamaz")
    @Field("doctor_id")
    @Indexed
    private String doctorId;

    @NotNull(message = "Tarih boş olamaz")
    @Indexed
    private LocalDate date;

    @NotNull(message = "Başlangıç saati boş olamaz")
    @Field("start_time")
    private LocalTime startTime;

    @NotNull(message = "Bitiş saati boş olamaz")
    @Field("end_time")
    private LocalTime endTime;

    @NotNull(message = "Süre belirtilmelidir")
    @Min(value = 15, message = "Slot süresi en az 15 dakika olmalı")
    @Field("duration_minutes")
    private Integer durationMinutes;

    @NotNull(message = "Slot durumu belirtilmelidir")
    @Indexed
    private SlotStatus status;

    @Field("appointment_id")
    private String appointmentId; // Rezerve edildiyse

    @Field("blocked_reason")
    private String blockedReason; // Bloklandıysa nedeni

    @Field("blocked_by")
    private String blockedBy;

    @Field("blocked_at")
    private LocalDateTime blockedAt;

    @Field("is_recurring")
    private Boolean isRecurring = false;

    @Field("recurrence_pattern")
    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY

    @Field("consultation_types")
    private Set<ConsultationType> allowedConsultationTypes;
    
    // === CONSULTATION TYPE ENUM ===
    public enum ConsultationType {
        IN_PERSON,      // Yüz yüze
        VIDEO_CALL,     // Video görüşme
        PHONE_CALL,     // Telefon görüşmesi
        FOLLOW_UP       // Kontrol
    }

    // === ENUM TANIMI ===
    public enum SlotStatus {
        AVAILABLE,      // Müsait
        BOOKED,         // Rezerve edildi
        BLOCKED,        // Bloklandı (doktor tarafından)
        UNAVAILABLE,    // Müsait değil (sistem tarafından)
        EXPIRED         // Süresi doldu
    }

    // === HELPER METHODS ===


    public boolean isAvailable() {
        // Sadece AVAILABLE status’teki ve gelecekteki slotlar müsait kabul edilir.
        if (status != SlotStatus.AVAILABLE) {
            return false; // booked veya blocked ise false
        }
        
        LocalDateTime slotStart = LocalDateTime.of(date, startTime);
        return slotStart.isAfter(LocalDateTime.now());
    }

    //public boolean isAvailable() {
    //    return status.equals(SlotStatus.AVAILABLE) && 
    //           date.atTime(startTime).isAfter(LocalDateTime.now());
    //}

    public boolean isPast() {
        return date.atTime(endTime).isBefore(LocalDateTime.now());
    }

    public void book(String appointmentId) {
        this.status = SlotStatus.BOOKED;
        this.appointmentId = appointmentId;
    }

    public void block(String reason, String blockedBy) {
        this.status = SlotStatus.BLOCKED;
        this.blockedReason = reason;
        this.blockedBy = blockedBy;
        this.blockedAt = LocalDateTime.now();
    }

    public void makeAvailable() {
        this.status = SlotStatus.AVAILABLE;
        this.appointmentId = null;
        this.blockedReason = null;
        this.blockedBy = null;
        this.blockedAt = null;
    }
}