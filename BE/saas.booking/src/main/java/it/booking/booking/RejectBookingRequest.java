package it.booking.booking;

import jakarta.validation.constraints.Size;

public record RejectBookingRequest(
        @Size(max = 255)
        String reason
) {
}
