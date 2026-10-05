package com.landingpage.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        return generateToken(principal, null);
    }

    public String generateToken(UserPrincipal principal, UUID sessionId) {
        Instant now = Instant.now();
        List<String> roles = principal.authorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        var builder = Jwts.builder()
                .subject(principal.email())
                .claim("uid", principal.id().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)));
        if (sessionId != null) {
            builder.claim("sid", sessionId.toString());
        }
        return builder.signWith(signingKey).compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("uid", String.class));
    }

    public UUID extractSessionId(String token) {
        String value = parseClaims(token).get("sid", String.class);
        return value == null ? null : UUID.fromString(value);
    }

    public boolean isValid(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equalsIgnoreCase(principal.email())
                && claims.getExpiration().after(new Date())
                && claims.get("uid", String.class).equals(principal.id().toString());
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
