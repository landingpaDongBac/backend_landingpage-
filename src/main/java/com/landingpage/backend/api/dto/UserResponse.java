package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.Role;

import java.util.Set;
import java.util.UUID;
import java.time.Instant;

public record UserResponse(UUID id, String email, String displayName, Set<Role> roles,
                           boolean enabled, Instant lastLoginAt) {
}
