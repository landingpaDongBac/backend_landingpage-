package com.landingpage.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 255) String displayName
) {
}
