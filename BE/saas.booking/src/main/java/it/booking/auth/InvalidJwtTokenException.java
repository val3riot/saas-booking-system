package it.booking.auth;

import io.jsonwebtoken.JwtException;

public class InvalidJwtTokenException extends JwtException {

    public InvalidJwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
