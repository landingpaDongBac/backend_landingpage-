package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.AuthResponse;
import com.landingpage.backend.api.dto.ChangePasswordRequest;
import com.landingpage.backend.api.dto.LoginRequest;
import com.landingpage.backend.api.dto.UserResponse;
import com.landingpage.backend.domain.Role;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.UnauthorizedException;
import com.landingpage.backend.repository.UserRepository;
import com.landingpage.backend.security.JwtService;
import com.landingpage.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> new UnauthorizedException("AUTHENTICATION_FAILED", "Authentication failed"));
            user.setLastLoginAt(Instant.now());
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(user);
            auditLogService.record(email, "AUTH_LOGIN_SUCCESS", "USER", user.getId(), "Successful login");
            return response(UserPrincipal.from(user), refresh);
        } catch (AuthenticationException exception) {
            registerFailure(email);
            auditLogService.record(email, "AUTH_LOGIN_FAILURE", "USER", null, "Failed login attempt");
            throw exception;
        }
    }

    public AuthResponse refresh(String rawToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawToken);
        if (rotation.replay()) {
            auditLogService.record(null, "AUTH_REFRESH_REPLAY", "SESSION", rotation.familyId(),
                    "Refresh-token replay detected; token family revoked");
            throw new UnauthorizedException("REFRESH_TOKEN_REUSED", "Refresh token has already been used");
        }
        User user = rotation.user();
        auditLogService.record(user.getEmail(), "AUTH_REFRESH", "SESSION", rotation.familyId(), "Session refreshed");
        return response(UserPrincipal.from(user), rotation.issuedToken());
    }

    public void logout(String rawToken) {
        String email = refreshTokenService.revoke(rawToken);
        auditLogService.record(email, "AUTH_LOGOUT", "SESSION", null, "Session revoked");
    }

    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("AUTHENTICATION_REQUIRED", "Authentication required"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        refreshTokenService.revokeAll(user.getId());
        auditLogService.record(user.getEmail(), "AUTH_PASSWORD_CHANGED", "USER", user.getId(), "Password changed");
    }

    public UserResponse currentUser(UserPrincipal principal) {
        return toResponse(principal);
    }

    private UserResponse toResponse(UserPrincipal principal) {
        Set<Role> roles = principal.authorities().stream()
                .map(authority -> Role.valueOf(authority.getAuthority().replaceFirst("^ROLE_", "")))
                .collect(Collectors.toUnmodifiableSet());
        User user = userRepository.findById(principal.id()).orElse(null);
        return new UserResponse(principal.id(), principal.email(), user == null ? null : user.getDisplayName(),
                roles, principal.enabled(), user == null ? null : user.getLastLoginAt());
    }

    private AuthResponse response(UserPrincipal principal, RefreshTokenService.IssuedToken refresh) {
        return new AuthResponse(jwtService.generateToken(principal, refresh.familyId()), refresh.rawToken(),
                "Bearer", jwtService.getExpirationSeconds(), refreshTokenService.expirationSeconds(),
                toResponse(principal));
    }

    private void registerFailure(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
                user.setFailedLoginAttempts(0);
            }
            userRepository.save(user);
        });
    }
}
