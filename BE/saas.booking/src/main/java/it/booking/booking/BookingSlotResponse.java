package it.booking.booking;

import java.time.Instant;
import java.time.LocalDate;

public record BookingSlotResponse(
        Long providerId,
        Long serviceId,
        LocalDate date,
        Instant startsAt,
        Instant endsAt,
        BookingSlotStatus status
) {
}
