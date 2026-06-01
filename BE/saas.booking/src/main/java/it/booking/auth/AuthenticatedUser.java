package it.booking.auth;

import it.booking.user.UserRole;

public record AuthenticatedUser(Long id, String email, UserRole role) {
}
