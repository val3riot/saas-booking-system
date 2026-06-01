package it.booking.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateBookingRequest(
        @NotNull Long providerId,
        @NotNull Long serviceId,
        @NotNull @Future Instant startsAt
) {
}
