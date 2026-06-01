package it.booking.availability;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateAvailabilityExceptionRequest(
        Long serviceId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Size(max = 255) String reason
) {
}
