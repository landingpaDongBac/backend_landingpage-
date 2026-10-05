package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        boolean enabled,
        boolean locked,
        Instant lockedUntil,
        Set<Role> roles,
        Instant lastLoginAt,
        Instant passwordChangedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
