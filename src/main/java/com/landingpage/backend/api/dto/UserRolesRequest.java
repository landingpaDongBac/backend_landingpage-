package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UserRolesRequest(@NotEmpty Set<Role> roles) {
}
