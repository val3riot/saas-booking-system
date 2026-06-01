package it.booking.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long id,
        Long providerId,
        Long serviceId,
        @Schema(description = "ISO day of week: 1=Monday, 7=Sunday", example = "1")
        short dayOfWeek,
        @Schema(type = "string", format = "time", example = "09:00")
        LocalTime startTime,
        @Schema(type = "string", format = "time", example = "12:00")
        LocalTime endTime,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
