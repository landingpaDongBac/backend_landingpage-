package com.landingpage.backend.service;

import com.landingpage.backend.domain.RefreshToken;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.exception.UnauthorizedException;
import com.landingpage.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository repository;

    @Value("${app.jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Transactional
    public IssuedToken issue(User user) {
        return create(user, UUID.randomUUID());
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("REFRESH_TOKEN_INVALID", "Refresh token is invalid"));
        if (current.getRevokedAt() != null) {
            repository.revokeFamily(current.getFamilyId(), now);
            return RotationResult.replayDetected(current.getFamilyId());
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            repository.save(current);
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }
        User user = current.getUser();
        if (!user.isEnabled() || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw new UnauthorizedException("ACCOUNT_UNAVAILABLE", "Account is disabled or locked");
        }
        IssuedToken replacement = create(user, current.getFamilyId());
        current.setRevokedAt(now);
        current.setLastUsedAt(now);
        current.setReplacedByTokenHash(hash(replacement.rawToken()));
        repository.save(current);
        return RotationResult.success(user, replacement);
    }

    @Transactional
    public String revoke(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken)).orElse(null);
        if (token == null) return null;
        repository.revokeFamily(token.getFamilyId(), Instant.now());
        return token.getUser().getEmail();
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public boolean isSessionActive(UUID familyId, UUID userId) {
        return repository.existsByFamilyIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(familyId, userId, Instant.now());
    }

    public long expirationSeconds() { return refreshExpirationMs / 1000; }

    private IssuedToken create(User user, UUID familyId) {
        String raw = randomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        repository.save(token);
        return new IssuedToken(raw, familyId, token.getExpiresAt());
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedToken(String rawToken, UUID familyId, Instant expiresAt) { }

    public record RotationResult(boolean replay, User user, IssuedToken issuedToken, UUID familyId) {
        static RotationResult success(User user, IssuedToken token) {
            return new RotationResult(false, user, token, token.familyId());
        }
        static RotationResult replayDetected(UUID familyId) {
            return new RotationResult(true, null, null, familyId);
        }
    }
}
