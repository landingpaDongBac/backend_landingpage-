package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.MediaResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MediaConfirmRequest(
        @NotBlank @Size(max = 500) String publicId,
        @NotNull MediaResourceType resourceType,
        @Size(max = 500) String originalFileName
) {
}
