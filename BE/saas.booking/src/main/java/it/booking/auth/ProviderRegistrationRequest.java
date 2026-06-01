package it.booking.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderRegistrationRequest(
        @Email @NotBlank String email,
        @NotBlank @ValidPassword String password,
        @NotBlank @Size(max = 255) String businessName,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 120) String category,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 255) String address
) {
}
