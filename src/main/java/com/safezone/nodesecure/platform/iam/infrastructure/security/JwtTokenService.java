package com.safezone.nodesecure.platform.iam.infrastructure.security;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.TokenClaims;
import com.safezone.nodesecure.platform.iam.domain.services.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues identity-only tokens: user id, organization id and token version.
 * Role, permissions and warehouses are not stored in the token (they are read from the database).
 */
@Component
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;

    public JwtTokenService(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration-minutes:480}") long minutes,
                           Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(Math.max(1, minutes));
        this.clock = clock;
    }

    @Override
    public String createToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getUsername().value())
                .claim("userId", user.getId())
                .claim("companyId", user.getCompanyId())
                .claim("tokenVersion", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<TokenClaims> readToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long tokenVersion = toLong(claims.get("tokenVersion"));
            if (tokenVersion == null) {
                // Tokens issued before the multi-tenant model carry roles in the payload: rejected.
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(
                    toLong(claims.get("userId")),
                    claims.getSubject(),
                    toLong(claims.get("companyId")),
                    tokenVersion));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
