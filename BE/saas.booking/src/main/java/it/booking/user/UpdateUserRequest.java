package it.booking.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @Email @NotBlank String email,
        @NotNull UserRole role,
        @NotNull Boolean enabled
) {
}
