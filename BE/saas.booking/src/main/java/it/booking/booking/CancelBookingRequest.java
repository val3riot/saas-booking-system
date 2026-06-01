package it.booking.booking;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 255)
        String reason
) {
}
