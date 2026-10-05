package com.landingpage.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void createsAndValidatesToken() {
        JwtService service = new JwtService("unit-test-secret", 60_000);
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "admin@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String token = service.generateToken(principal);

        assertThat(service.extractUsername(token)).isEqualTo("admin@example.com");
        assertThat(service.extractUserId(token)).isEqualTo(principal.id());
        assertThat(service.isValid(token, principal)).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService("unit-test-secret", -1);
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "admin@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String token = service.generateToken(principal);

        assertThatThrownBy(() -> service.isValid(token, principal))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
