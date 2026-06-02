package it.booking.booking;

import java.util.List;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public static List<BookingStatus> blockingStatuses() {
        return List.of(PENDING, CONFIRMED);
    }
}
