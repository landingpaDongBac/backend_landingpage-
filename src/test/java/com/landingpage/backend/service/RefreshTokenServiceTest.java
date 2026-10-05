package com.landingpage.backend.service;

import com.landingpage.backend.domain.RefreshToken;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock RefreshTokenRepository repository;
    RefreshTokenService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 60_000L);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setEnabled(true);
        org.mockito.Mockito.lenient().when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issueStoresOnlyHashAndRotationRevokesPreviousToken() {
        var issued = service.issue(user);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(stored.getTokenHash()).isNotEqualTo(issued.rawToken()).hasSize(64);

        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        var rotation = service.rotate(issued.rawToken());

        assertThat(rotation.replay()).isFalse();
        assertThat(rotation.issuedToken().familyId()).isEqualTo(issued.familyId());
        assertThat(rotation.issuedToken().rawToken()).isNotEqualTo(issued.rawToken());
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void replayRevokesEntireTokenFamily() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(UUID.randomUUID());
        token.setTokenHash("a".repeat(64));
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevokedAt(Instant.now());
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        var result = service.rotate("already-used-token");

        assertThat(result.replay()).isTrue();
        verify(repository).revokeFamily(org.mockito.ArgumentMatchers.eq(token.getFamilyId()), any());
    }
}
