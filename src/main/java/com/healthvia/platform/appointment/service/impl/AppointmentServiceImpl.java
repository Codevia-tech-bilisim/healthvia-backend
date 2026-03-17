package com.healthvia.platform.appointment.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.appointment.dto.VideoAppointmentRequest;
import com.healthvia.platform.appointment.dto.VideoAppointmentResponse;
import com.healthvia.platform.appointment.entity.Appointment;
import com.healthvia.platform.appointment.entity.Appointment.AppointmentStatus;
import com.healthvia.platform.appointment.entity.Appointment.ConsultationType;
import com.healthvia.platform.appointment.entity.Appointment.MeetingInfo;
import com.healthvia.platform.appointment.entity.TimeSlot;
import com.healthvia.platform.appointment.entity.TimeSlot.SlotStatus;
import com.healthvia.platform.appointment.repository.AppointmentRepository;
import com.healthvia.platform.appointment.service.AppointmentService;
import com.healthvia.platform.appointment.service.TimeSlotService;
import com.healthvia.platform.common.exception.AppointmentExceptions;
import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.zoom.dto.ZoomMeetingRequest;
import com.healthvia.platform.zoom.dto.ZoomMeetingResponse;
import com.healthvia.platform.zoom.service.ZoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotService timeSlotService;
    private final ZoomService zoomService;

    // === TEMEL CRUD İŞLEMLERİ ===

    @Override
    public Appointment createAppointment(String patientId, String doctorId, 
                                        String slotId, String chiefComplaint) {
        
        // Slot kontrolü
        TimeSlot slot = timeSlotService.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot bulunamadı: " + slotId));
        
        // Geçmiş tarih kontrolü
        LocalDate appointmentDate = slot.getDate();
        if (appointmentDate.isBefore(LocalDate.now())) {
            throw new AppointmentExceptions.PastDateAppointmentException();
        }
        
        // Slot müsaitlik kontrolü
        if (!slot.isAvailable()) {
            if (slot.getStatus() == SlotStatus.BOOKED) {
                throw new AppointmentExceptions.SlotAlreadyBookedException(slotId);
            }
            else {
                throw new AppointmentExceptions.SlotNotAvailableException(slotId);
            }
        } 

        // Appointment nesnesi oluştur
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setChiefComplaint(chiefComplaint);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setStatusChangedAt(LocalDateTime.now());

        // Slot’u BOOKED yap ve kaydet
        slot.setStatus(SlotStatus.BOOKED);
        timeSlotService.updateSlot(slot.getId(), slot); 

        log.info("Creating new appointment for patient: {} with doctor: {}", 
                    patientId, doctorId);

        // Validasyon
        validateAppointment(appointment);

        return appointmentRepository.save(appointment);
    }



    @Override
    @Transactional(readOnly = true)
    public Optional<Appointment> findById(String id) {
        return appointmentRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Appointment updateAppointment(String id, Appointment appointment) {
        Appointment existingAppointment = findByIdOrThrow(id);
        
        // Güvenli güncelleme alanları
        if (appointment.getDoctorNotes() != null) {
            existingAppointment.setDoctorNotes(appointment.getDoctorNotes());
        }
        if (appointment.getPatientNotes() != null) {
            existingAppointment.setPatientNotes(appointment.getPatientNotes());
        }
        if (appointment.getPrescriptionId() != null) {
            existingAppointment.setPrescriptionId(appointment.getPrescriptionId());
        }
        
        return appointmentRepository.save(existingAppointment);
    }

    @Override
    public void deleteAppointment(String id, String deletedBy) {
        Appointment appointment = findByIdOrThrow(id);
        appointment.markAsDeleted(deletedBy);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    // === RANDEVU ALMA İŞLEMLERİ ===

    @Override
    public Appointment bookAppointment(String patientId, String doctorId, String slotId,
                                     String treatmentTypeId, String chiefComplaint) {
        log.info("Booking appointment for patient: {} with doctor: {} for slot: {}", 
                patientId, doctorId, slotId);

        // 1. Slot kontrolü ve rezervasyonu
        TimeSlot slot = timeSlotService.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Slot bulunamadı: " + slotId));


        if (!slot.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Bu slot belirtilen doktora ait değil");
        }

        // 2. Çakışma kontrolü
        LocalDateTime startDateTime = slot.getDate().atTime(slot.getStartTime());
        LocalDateTime endDateTime = slot.getDate().atTime(slot.getEndTime());

        LocalDateTime slotDateTime = slot.getDate().atTime(slot.getStartTime());
        if (slotDateTime.isBefore(LocalDateTime.now())) {
            throw new AppointmentExceptions.PastDateAppointmentException();
        }

        // Slot müsaitlik kontrolü
        if (!slot.isAvailable()) {
            if (slot.getStatus() == SlotStatus.BOOKED) {
                // Slot zaten rezerve edilmiş
                throw new AppointmentExceptions.SlotAlreadyBookedException(slot.getId());
            } else {
                // Slot başka bir nedenle kullanılamıyor
                throw new AppointmentExceptions.SlotNotAvailableException(slot.getId());
            }
        }

        // 3. Randevu oluştur
        Appointment appointment = Appointment.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentDate(slot.getDate())
            .startTime(slot.getStartTime())
            .endTime(slot.getEndTime())
            .durationMinutes(slot.getDurationMinutes())
            .status(AppointmentStatus.CONFIRMED)
            .confirmedAt(LocalDateTime.now())
            .confirmedBy("SYSTEM")
            .consultationType(Appointment.ConsultationType.IN_PERSON)
            .treatmentTypeId(treatmentTypeId)
            .chiefComplaint(chiefComplaint)
            .consultationFee(java.math.BigDecimal.ZERO)
            .currency("TRY")
            .paymentStatus(Appointment.PaymentStatus.PENDING)
            .smsNotificationsEnabled(true)
            .emailNotificationsEnabled(true)
            .statusChangedAt(LocalDateTime.now())
            .build();

        appointment = appointmentRepository.save(appointment);

        // 4. Slot'u rezerve et
        timeSlotService.bookSlot(slotId, appointment.getId());

        log.info("Appointment booked successfully with ID: {}", appointment.getId());
        return appointment;
    }

    @Override
    public Appointment confirmAppointment(String appointmentId, String confirmedBy) {
        log.info("Confirming appointment: {} by: {}", appointmentId, confirmedBy);

        Appointment appointment = findByIdOrThrow(appointmentId);
        
        if (!appointment.getStatus().equals(AppointmentStatus.PENDING)) {
            throw new RuntimeException("Bu randevu zaten onaylanmış veya farklı bir durumda");
        }

        appointment.confirm(confirmedBy);
        return appointmentRepository.save(appointment);
    }


    @Override
    public Appointment cancelAppointment(String appointmentId, String cancelledBy, String reason) {
        // Randevuyu al
        Appointment appointment = findByIdOrThrow(appointmentId);
        
        // 24 saat kontrolü
        LocalDateTime appointmentDateTime = LocalDateTime.of(
            appointment.getAppointmentDate(), 
            appointment.getStartTime()
        );
        
        boolean isCancelledByDoctor = appointment.getDoctorId().equals(cancelledBy)
            || "admin".equalsIgnoreCase(cancelledBy);

        if (!isCancelledByDoctor) {
            long hoursUntilAppointment = ChronoUnit.HOURS.between(LocalDateTime.now(), appointmentDateTime);
            if (hoursUntilAppointment < 24) {
                throw new AppointmentExceptions.CancellationDeadlineException(hoursUntilAppointment);
            }
        }

        log.info("Cancelling appointment: {} by: {}", appointmentId, cancelledBy);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Bu randevu iptal edilemez");
        }

        // İptal işlemi
        appointment.cancel(cancelledBy, reason);

        // Kaydet ve geri dön
        return appointmentRepository.save(appointment);
    }


    

    @Override
    public Appointment checkInPatient(String appointmentId) {
        log.info("Checking in patient for appointment: {}", appointmentId);
        
        Appointment appointment = findByIdOrThrow(appointmentId);
        
        if (!appointment.getStatus().equals(AppointmentStatus.CONFIRMED)) {
            throw new RuntimeException("Check-in için randevu onaylanmış olmalı");
        }
        
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointment.setCheckedInAt(LocalDateTime.now());
        appointment.setStatusChangedAt(LocalDateTime.now());
        
        return appointmentRepository.save(appointment);
    }

    @Override
    public Appointment startConsultation(String appointmentId, String doctorId) {
        log.info("Starting consultation for appointment: {} by doctor: {}", appointmentId, doctorId);

        Appointment appointment = findByIdOrThrow(appointmentId);
        
        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Bu randevu size ait değil");
        }

        if (!appointment.getStatus().equals(AppointmentStatus.CONFIRMED) &&
            !appointment.getStatus().equals(AppointmentStatus.CHECKED_IN)) {
            throw new RuntimeException("Muayene başlatmak için randevu onaylanmış veya check-in yapılmış olmalı");
        }

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setConsultationStartedAt(LocalDateTime.now());
        appointment.setStatusChangedAt(LocalDateTime.now());
        appointment.setStatusChangedBy(doctorId);

        return appointmentRepository.save(appointment);
    }

    @Override
    public Appointment completeAppointment(String appointmentId, String doctorId, 
                                         String notes, String prescriptionId) {
        log.info("Completing appointment: {} by doctor: {}", appointmentId, doctorId);

        Appointment appointment = findByIdOrThrow(appointmentId);
        
        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Bu randevu size ait değil");
        }

        if (!appointment.getStatus().equals(AppointmentStatus.IN_PROGRESS)) {
            throw new RuntimeException("Tamamlanacak randevu 'devam ediyor' durumunda olmalı");
        }

        appointment.markAsCompleted(doctorId);
        appointment.setDoctorNotes(notes);
        appointment.setPrescriptionId(prescriptionId);

        return appointmentRepository.save(appointment);
    }

    // === EKSİK METODLAR ===

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findConflictingAppointments(String doctorId, LocalDate date, 
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        return appointmentRepository.findConflictingAppointments(
            doctorId, date, startTime.toLocalTime(), endTime.toLocalTime());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConflictingAppointment(String doctorId, LocalDate date, 
                                           LocalDateTime startTime, LocalDateTime endTime) {
        List<Appointment> conflicting = findConflictingAppointments(doctorId, date, startTime, endTime);
        return !conflicting.isEmpty();
    }

    @Override
    public Appointment rescheduleAppointment(String appointmentId, String newSlotId, String rescheduledBy) {
        throw new RuntimeException("Erteleme özelliği henüz aktif değil");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> findByPatientId(String patientId, Pageable pageable) {
        return appointmentRepository.findByPatientIdAndDeletedFalse(patientId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> findByDoctorId(String doctorId, Pageable pageable) {
        return appointmentRepository.findByDoctorIdAndDeletedFalse(doctorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findByStatusAndDeletedFalse(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByAppointmentDateBetweenAndDeletedFalse(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByDoctorAndDate(String doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndAppointmentDateAndDeletedFalse(doctorId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPatientAndDateRange(String patientId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByPatientIdAndAppointmentDateBetweenAndDeletedFalse(
            patientId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findUpcomingAppointments(String userId, int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        return appointmentRepository.findUpcomingAppointments(userId, LocalDate.now(), endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findTodayAppointments(String doctorId) {
        return findByDoctorAndDate(doctorId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsForReminder(LocalDateTime reminderTime) {
        LocalDate reminderDate = reminderTime.toLocalDate();
        return appointmentRepository.findAppointmentsForReminder(reminderDate, reminderDate.plusDays(1));
    }

    @Override
    public void sendAppointmentReminder(String appointmentId) {
        log.info("Sending reminder for appointment: {}", appointmentId);
        markReminderSent(appointmentId);
    }

    @Override
    public void markReminderSent(String appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.setReminderSentAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatusAndDeletedFalse(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAppointmentsByDoctor(String doctorId) {
        return appointmentRepository.countByDoctorIdAndDeletedFalse(doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAppointmentsByPatient(String patientId) {
        return appointmentRepository.countByPatientIdAndDeletedFalse(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateCompletionRate(String doctorId, LocalDate startDate, LocalDate endDate) {
        long totalAppointments = appointmentRepository
            .countByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(doctorId, startDate, endDate);
        
        if (totalAppointments == 0) return 0.0;
        
        long completedAppointments = appointmentRepository
            .countByDoctorIdAndStatusAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, AppointmentStatus.COMPLETED, startDate, endDate);
        
        return (double) completedAppointments / totalAppointments * 100;
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateNoShowRate(String doctorId, LocalDate startDate, LocalDate endDate) {
        long totalAppointments = appointmentRepository
            .countByDoctorIdAndAppointmentDateBetweenAndDeletedFalse(doctorId, startDate, endDate);
        
        if (totalAppointments == 0) return 0.0;
        
        long noShowAppointments = appointmentRepository
            .countByDoctorIdAndStatusAndAppointmentDateBetweenAndDeletedFalse(
                doctorId, AppointmentStatus.NO_SHOW, startDate, endDate);
        
        return (double) noShowAppointments / totalAppointments * 100;
    }

    @Override
    public Appointment updateAppointmentStatus(String appointmentId, AppointmentStatus status, 
                                             String updatedBy, String reason) {
        log.info("Updating appointment {} status to {} by {}", appointmentId, status, updatedBy);
        
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.setPreviousStatus(appointment.getStatus());
        appointment.setStatus(status);
        appointment.setStatusChangedAt(LocalDateTime.now());
        appointment.setStatusChangedBy(updatedBy);
        appointment.setStatusChangeReason(reason);
        
        return appointmentRepository.save(appointment);
    }

    // === HELPER METHODS ===

    private Appointment findByIdOrThrow(String id) {
        return findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Randevu bulunamadı: " + id));
    }

    private void validateAppointment(Appointment appointment) {
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Geçmiş tarihli randevu oluşturulamaz");
        }

        if (appointment.getStartTime().isAfter(appointment.getEndTime())) {
            throw new RuntimeException("Başlangıç saati bitiş saatinden sonra olamaz");
        }

        if (appointment.getDurationMinutes() < 15 || appointment.getDurationMinutes() > 180) {
            throw new RuntimeException("Randevu süresi 15-180 dakika arasında olmalı");
        }
    }

    // === VIDEO RANDEVU İŞLEMLERİ ===

    @Override
    public VideoAppointmentResponse createVideoAppointment(VideoAppointmentRequest request) {
        log.info("Creating video appointment for patient: {} with doctor: {}",
                request.getPatientId(), request.getDoctorId());

        // 1. Tarih validasyonu
        LocalDateTime appointmentDateTime = request.getAppointmentDate().atTime(request.getStartTime());
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new AppointmentExceptions.PastDateAppointmentException();
        }

        // 2. Çakışma kontrolü
        LocalTime endTime = request.getStartTime().plusMinutes(request.getDurationMinutes());
        boolean hasConflict = hasConflictingAppointment(
                request.getDoctorId(),
                request.getAppointmentDate(),
                appointmentDateTime,
                request.getAppointmentDate().atTime(endTime)
        );

        if (hasConflict) {
            throw new RuntimeException("Bu saat diliminde başka bir randevu mevcut");
        }

        // 3. Zoom meeting oluştur
        String meetingTopic = request.getMeetingTopic() != null
                ? request.getMeetingTopic()
                : "HealthVia Video Konsültasyon";

        ZoomMeetingRequest zoomRequest = ZoomMeetingRequest.builder()
                .topic(meetingTopic)
                .startTime(appointmentDateTime)
                .durationMinutes(request.getDurationMinutes())
                .agenda(request.getMeetingAgenda())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .waitingRoom(request.getWaitingRoom())
                .build();

        ZoomMeetingResponse zoomResponse = zoomService.createMeeting(zoomRequest);

        // 4. Appointment oluştur
        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .durationMinutes(request.getDurationMinutes())
                .status(AppointmentStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .confirmedBy("SYSTEM")
                .consultationType(ConsultationType.VIDEO_CALL)
                .treatmentTypeId(request.getTreatmentTypeId())
                .chiefComplaint(request.getChiefComplaint())
                .consultationFee(java.math.BigDecimal.ZERO)
                .currency("TRY")
                .paymentStatus(Appointment.PaymentStatus.PENDING)
                .isFollowUp(request.getIsFollowUp())
                .originalAppointmentId(request.getOriginalAppointmentId())
                .smsNotificationsEnabled(true)
                .emailNotificationsEnabled(true)
                .statusChangedAt(LocalDateTime.now())
                .meetingInfo(new MeetingInfo(
                        String.valueOf(zoomResponse.getId()),
                        zoomResponse.getJoinUrl(),
                        zoomResponse.getPassword(),
                        "ZOOM",
                        null,
                        null,
                        null
                ))
                .build();

        appointment = appointmentRepository.save(appointment);
        log.info("Video appointment created successfully with ID: {} and Zoom meeting ID: {}",
                appointment.getId(), zoomResponse.getId());

        // 5. Response oluştur (doktor için startUrl dahil)
        VideoAppointmentResponse response = VideoAppointmentResponse.fromAppointment(appointment);
        if (response.getMeetingDetails() != null) {
            response.getMeetingDetails().setStartUrl(zoomResponse.getStartUrl());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public VideoAppointmentResponse getMeetingLink(String appointmentId, String userId, String userRole) {
        log.info("Getting meeting link for appointment: {} by user: {} with role: {}",
                appointmentId, userId, userRole);

        Appointment appointment = findByIdOrThrow(appointmentId);

        // Yetki kontrolü
        if (!appointment.getPatientId().equals(userId) &&
            !appointment.getDoctorId().equals(userId) &&
            !"ADMIN".equalsIgnoreCase(userRole)) {
            throw new RuntimeException("Bu randevunun meeting bilgilerine erişim yetkiniz yok");
        }

        // Video randevu kontrolü
        if (appointment.getConsultationType() != ConsultationType.VIDEO_CALL) {
            throw new RuntimeException("Bu randevu video konsültasyon değil");
        }

        if (appointment.getMeetingInfo() == null) {
            throw new RuntimeException("Bu randevu için meeting bilgisi bulunamadı");
        }

        // Doktor ise startUrl'i Zoom'dan al
        if (appointment.getDoctorId().equals(userId) || "ADMIN".equalsIgnoreCase(userRole)) {
            try {
                ZoomMeetingResponse zoomMeeting = zoomService.getMeeting(
                        appointment.getMeetingInfo().getMeetingId());
                return VideoAppointmentResponse.fromAppointmentForDoctor(appointment, zoomMeeting.getStartUrl());
            } catch (Exception e) {
                log.warn("Could not fetch Zoom meeting details: {}", e.getMessage());
                return VideoAppointmentResponse.fromAppointment(appointment);
            }
        }

        return VideoAppointmentResponse.fromAppointment(appointment);
    }

    @Override
    public Appointment cancelVideoAppointment(String appointmentId, String cancelledBy, String reason) {
        log.info("Cancelling video appointment: {} by: {}", appointmentId, cancelledBy);

        Appointment appointment = findByIdOrThrow(appointmentId);

        // Video randevu ise Zoom meeting'i sil
        if (appointment.getConsultationType() == ConsultationType.VIDEO_CALL &&
            appointment.getMeetingInfo() != null &&
            appointment.getMeetingInfo().getMeetingId() != null) {

            try {
                zoomService.deleteMeeting(appointment.getMeetingInfo().getMeetingId());
                log.info("Zoom meeting deleted for appointment: {}", appointmentId);
            } catch (Exception e) {
                log.warn("Could not delete Zoom meeting: {}", e.getMessage());
                // Meeting silinemese bile randevu iptal edilmeli
            }
        }

        // Normal iptal işlemini çağır
        return cancelAppointment(appointmentId, cancelledBy, reason);
    }
}