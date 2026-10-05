package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminUserCreateRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 255) String displayName,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotEmpty Set<Role> roles
) {
}
