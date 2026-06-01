package it.booking.offering;

import java.time.Instant;

public record OfferedServiceResponse(
        Long id,
        Long providerId,
        String name,
        String description,
        int durationMinutes,
        int priceCents,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
