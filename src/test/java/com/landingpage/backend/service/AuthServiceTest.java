package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.ChangePasswordRequest;
import com.landingpage.backend.api.dto.LoginRequest;
import com.landingpage.backend.domain.Role;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.repository.UserRepository;
import com.landingpage.backend.security.JwtService;
import com.landingpage.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserRepository users;
    @Mock RefreshTokenService refreshTokens;
    @Mock PasswordEncoder encoder;
    @Mock AuditLogService audit;
    @Mock Authentication authentication;
    AuthService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, jwtService, users, refreshTokens, encoder, audit);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setPasswordHash("hash");
        user.setEnabled(true);
        user.setRoles(Set.of(Role.ADMIN));
    }

    @Test
    void loginTracksLastLoginAndIssuesTokenPair() {
        UserPrincipal principal = UserPrincipal.from(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        var issued = new RefreshTokenService.IssuedToken("refresh", UUID.randomUUID(), Instant.now().plusSeconds(60));
        when(refreshTokens.issue(user)).thenReturn(issued);
        when(refreshTokens.expirationSeconds()).thenReturn(60L);
        when(jwtService.generateToken(any(), any())).thenReturn("access");
        when(jwtService.getExpirationSeconds()).thenReturn(15L);

        var response = service.login(new LoginRequest("ADMIN@example.com", "password"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(audit).record("admin@example.com", "AUTH_LOGIN_SUCCESS", "USER", user.getId(), "Successful login");
    }

    @Test
    void invalidCredentialsIncrementFailureCounterAndAreAudited() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(users).save(user);
        verify(audit).record("admin@example.com", "AUTH_LOGIN_FAILURE", "USER", null, "Failed login attempt");
    }

    @Test
    void passwordChangeRevokesAllRefreshSessions() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(encoder.matches("old-password", "hash")).thenReturn(true);
        when(encoder.matches("new-password-123", "hash")).thenReturn(false);
        when(encoder.encode("new-password-123")).thenReturn("new-hash");

        service.changePassword(UserPrincipal.from(user),
                new ChangePasswordRequest("old-password", "new-password-123"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokens).revokeAll(user.getId());
    }
}
