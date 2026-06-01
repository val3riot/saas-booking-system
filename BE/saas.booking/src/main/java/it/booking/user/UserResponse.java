package it.booking.user;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
