package it.booking.availability;

import java.time.Instant;

public record AvailabilityExceptionResponse(
        Long id,
        Long providerId,
        Long serviceId,
        Instant startsAt,
        Instant endsAt,
        String reason,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
