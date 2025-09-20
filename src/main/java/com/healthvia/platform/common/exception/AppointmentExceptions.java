// common/exception/AppointmentExceptions.java
package com.healthvia.platform.common.exception;

import com.healthvia.platform.common.constants.ErrorCodes;

public final class AppointmentExceptions {

    private AppointmentExceptions() {
        // Utility class, instance oluşturulmasını engelle
    }

    public static class SlotNotAvailableException extends BusinessException {
        public SlotNotAvailableException(String slotId) {
            super(ErrorCodes.APPOINTMENT_NOT_AVAILABLE, 
                  "Slot not available: %s. Bu zaman dilimi artık müsait değil. Lütfen başka bir zaman seçin.", 
                  slotId);
        }
    }

    public static class PastDateAppointmentException extends BusinessException {
        public PastDateAppointmentException() {
            super(ErrorCodes.INVALID_APPOINTMENT_DATE, 
                  "Geçmiş tarihli randevu oluşturulamaz");
        }
    }

    public static class SlotAlreadyBookedException extends BusinessException {
        public SlotAlreadyBookedException(String slotId) {
            super(ErrorCodes.APPOINTMENT_ALREADY_BOOKED, 
                  "Slot already booked: %s. Bu zaman dilimi zaten rezerve edilmiş", 
                  slotId);
        }
    }

    public static class CancellationDeadlineException extends BusinessException {
        public CancellationDeadlineException(long hoursLeft) {
            super(ErrorCodes.CANCELLATION_DEADLINE_PASSED, 
                  "Cancellation deadline passed. Randevu iptal edilemez. Randevuya %d saat kaldı. En az 24 saat öncesinden iptal edilmelidir.", 
                  hoursLeft);
        }
    }
}