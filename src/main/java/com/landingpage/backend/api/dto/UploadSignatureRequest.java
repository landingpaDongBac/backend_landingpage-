package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.MediaResourceType;
import jakarta.validation.constraints.NotNull;

public record UploadSignatureRequest(@NotNull MediaResourceType resourceType) {
}
