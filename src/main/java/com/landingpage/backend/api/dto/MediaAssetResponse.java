package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.MediaResourceType;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        String publicId,
        String cloudinaryPublicId,
        String cloudinaryAssetId,
        MediaResourceType resourceType,
        String format,
        String mimeType,
        String originalFileName,
        String secureUrl,
        Long fileSize,
        Integer width,
        Integer height,
        Double duration,
        Instant createdAt,
        String createdBy
) {
}
