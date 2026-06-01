package it.booking.auth;

import it.booking.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private static final int MIN_HMAC_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMillis;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationMillis,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience
    ) {
        validateConfiguration(secret, expirationMillis, issuer, audience);
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String issueToken(Long userId, String email, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .audience().add(audience).and()
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AuthenticatedUser(parseUserId(claims), claims.get("email", String.class), parseRole(claims));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtTokenException("Invalid JWT token", ex);
        }
    }

    private UserRole parseRole(Claims claims) {
        return UserRole.valueOf(claims.get("role", String.class));
    }

    private Long parseUserId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("JWT subject must be a user id", ex);
        }
    }

    private void validateConfiguration(String secret, long expirationMillis, String issuer, String audience) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret must not be blank");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_HMAC_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        if (expirationMillis <= 0) {
            throw new IllegalStateException("JWT expiration must be positive");
        }
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("JWT issuer must not be blank");
        }
        if (!StringUtils.hasText(audience)) {
            throw new IllegalStateException("JWT audience must not be blank");
        }
    }
}
