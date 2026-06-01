package it.booking.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CreateAvailabilityRequest(
        @Schema(description = "ISO day of week: 1=Monday, 7=Sunday", example = "1")
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        @Schema(type = "string", format = "time", example = "09:00")
        @NotNull LocalTime startTime,
        @Schema(type = "string", format = "time", example = "12:00")
        @NotNull LocalTime endTime
) {
}
