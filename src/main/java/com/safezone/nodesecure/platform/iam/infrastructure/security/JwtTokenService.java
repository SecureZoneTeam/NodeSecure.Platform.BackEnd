package com.safezone.nodesecure.platform.iam.infrastructure.security;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
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
import java.util.List;
import java.util.Optional;

@Component
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;

    public JwtTokenService(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration-days}") long days,
                           Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofDays(days);
        this.clock = clock;
    }

    @Override
    public String createToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getUsername().value())
                .claim("userId", user.getId())
                .claim("companyId", user.getCompanyId())
                .claim("warehouseId", user.getWarehouseId())
                .claim("roles", user.getRoleNames())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<SessionUser> readToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new SessionUser(
                    toLong(claims.get("userId")),
                    claims.getSubject(),
                    toLong(claims.get("companyId")),
                    toLong(claims.get("warehouseId")),
                    toRoles(claims.get("roles"))));
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

    private List<String> toRoles(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
