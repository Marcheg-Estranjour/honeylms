package com.honeygroup.honeylms.security;

import com.honeygroup.honeylms.user.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Generates and validates the JWTs used to authenticate API calls after login.
 * Library: jjwt 0.13.x (fluent API without "set" prefixes, verifyWith/parseSignedClaims).
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.expirationMs();
    }

    public String generateToken(UserAccount account) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(account.getEmail())
                .claim("userId", account.getId())
                .claim("role", account.getRole().getCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    /**
     * @return the token's claims if the signature is valid and it is not expired,
     *         empty otherwise. Never throws - callers just treat "empty" as "not authenticated".
     */
    public Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
