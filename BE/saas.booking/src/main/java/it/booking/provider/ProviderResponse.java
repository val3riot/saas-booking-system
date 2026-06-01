package it.booking.provider;

import java.time.Instant;

public record ProviderResponse(
        Long id,
        Long userId,
        String businessName,
        String description,
        String category,
        String city,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
