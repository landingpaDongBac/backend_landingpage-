package com.landingpage.backend.service;

import com.landingpage.backend.domain.Role;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock UserRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenService refreshTokens;
    @Mock AuditLogService audit;

    @Test
    void cannotDisableLastActiveAdmin() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        when(repository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(repository.countDistinctByEnabledTrueAndRolesContaining(Role.ADMIN)).thenReturn(1L);
        var service = new AdminUserService(repository, encoder, refreshTokens, audit);

        assertThatThrownBy(() -> service.setStatus(admin.getId(), false, false, "admin@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("last active ADMIN");
    }
}
