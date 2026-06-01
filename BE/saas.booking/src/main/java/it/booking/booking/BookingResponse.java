package it.booking.booking;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long customerId,
        Long providerId,
        String providerBusinessName,
        Long serviceId,
        String serviceName,
        Instant startsAt,
        Instant endsAt,
        BookingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
