package it.booking.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.booking.user.UserRole;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";
    private static final String ISSUER = "saas-booking-system-test";
    private static final String AUDIENCE = "saas-booking-api-test";

    @Test
    void issuedTokenUsesUserIdAsSubject() {
        JwtService jwtService = new JwtService(SECRET, 86_400_000, ISSUER, AUDIENCE);

        String token = jwtService.issueToken(42L, "user@example.com", UserRole.CUSTOMER);
        AuthenticatedUser user = jwtService.parseToken(token);

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void parseTokenRejectsWrongAudience() {
        JwtService issuerService = new JwtService(SECRET, 86_400_000, ISSUER, AUDIENCE);
        JwtService parserService = new JwtService(SECRET, 86_400_000, ISSUER, "another-api");

        String token = issuerService.issueToken(42L, "user@example.com", UserRole.CUSTOMER);

        assertThatThrownBy(() -> parserService.parseToken(token))
                .isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void constructorRejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 86_400_000, ISSUER, AUDIENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
