package com.healthvia.platform.common.exception;

import com.healthvia.platform.common.constants.ErrorCodes;

import java.util.Map;

public final class AppointmentExceptions { // final yapıyoruz, new'lenmesin

  private AppointmentExceptions() {
    // Utility class, instance oluşturulmasını engelle
  }

  public static class SlotNotAvailableException extends BusinessException {
    public SlotNotAvailableException(String slotId) {
      super(
              ErrorCodes.APPOINTMENT_NOT_AVAILABLE,
              Map.of("slotId", slotId,
                      "message", "Bu zaman dilimi artık müsait değil. Lütfen başka bir zaman seçin.")
      );
    }
  }

  public static class PastDateAppointmentException extends BusinessException {
    public PastDateAppointmentException() {
      super(
              ErrorCodes.INVALID_APPOINTMENT_DATE,
              Map.of("message", "Geçmiş tarihli randevu oluşturulamaz")
      );
    }
  }

  public static class SlotAlreadyBookedException extends BusinessException {
    public SlotAlreadyBookedException(String slotId) {
      super(
              ErrorCodes.APPOINTMENT_ALREADY_BOOKED,
              Map.of("slotId", slotId,
                      "message", "Bu zaman dilimi zaten rezerve edilmiş")
      );
    }
  }

  public static class CancellationDeadlineException extends BusinessException {
    public CancellationDeadlineException(long hoursLeft) {
      super(
              ErrorCodes.CANCELLATION_DEADLINE_PASSED,
              Map.of(
                      "hoursLeft", String.valueOf(hoursLeft),
                      "message", String.format("Randevu iptal edilemez. Randevuya %d saat kaldı. En az 24 saat öncesinden iptal edilmelidir.", hoursLeft)
              )
      );
    }
  }
}