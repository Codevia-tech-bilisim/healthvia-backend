
// src/main/java/com/healthvia/platform/appointment/exception/AppointmentExceptions.java
package com.healthvia.platform.appointment.exception;

import java.util.HashMap;
import java.util.Map;

import com.healthvia.platform.common.constants.ErrorCodes;
import com.healthvia.platform.common.exception.BusinessException;

public final class AppointmentExceptions {

    private AppointmentExceptions() {
        // Utility class, instance oluşturulmasını engelle
    }

    public static class SlotNotAvailableException extends BusinessException {
        public SlotNotAvailableException(String slotId) {
            super(
                ErrorCodes.APPOINTMENT_NOT_AVAILABLE,
                "Bu zaman dilimi artık müsait değil. Lütfen başka bir zaman seçin.",
                createDetails("slotId", slotId)
            );
        }
    }

    public static class PastDateAppointmentException extends BusinessException {
        public PastDateAppointmentException() {
            super(
                ErrorCodes.INVALID_APPOINTMENT_DATE,
                "Geçmiş tarihli randevu oluşturulamaz"
            );
        }
    }

    public static class SlotAlreadyBookedException extends BusinessException {
        public SlotAlreadyBookedException(String slotId) {
            super(
                ErrorCodes.APPOINTMENT_ALREADY_BOOKED,
                "Bu zaman dilimi zaten rezerve edilmiş",
                createDetails("slotId", slotId)
            );
        }
    }

    public static class CancellationDeadlineException extends BusinessException {
        public CancellationDeadlineException(long hoursLeft) {
            super(
                ErrorCodes.CANCELLATION_DEADLINE_PASSED,
                String.format("Randevu iptal edilemez. Randevuya %d saat kaldı. En az 24 saat öncesinden iptal edilmelidir.", hoursLeft),
                createDetails("hoursLeft", String.valueOf(hoursLeft))
            );
        }
    }
    
    // Utility method for creating details map
    private static Map<String, String> createDetails(String key, String value) {
        Map<String, String> details = new HashMap<>();
        details.put(key, value);
        return details;
    }

