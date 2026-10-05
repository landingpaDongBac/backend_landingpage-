package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.Role;

import java.util.List;

public record PermissionResponse(Role role, List<String> permissions) {
}
