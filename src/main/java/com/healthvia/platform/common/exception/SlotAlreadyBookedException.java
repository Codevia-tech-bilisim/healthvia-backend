package com.healthvia.platform.common.exception;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String slotId) {
        super("Slot already booked: " + slotId);
    }
}