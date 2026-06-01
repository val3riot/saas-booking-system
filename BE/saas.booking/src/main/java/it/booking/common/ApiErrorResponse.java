package it.booking.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        Map<String, ApiFieldError> fields
) {
}
