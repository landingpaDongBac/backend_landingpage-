package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.MediaResourceType;

import java.util.UUID;

public record PublicMediaResponse(
        UUID id,
        MediaResourceType resourceType,
        String mimeType,
        String secureUrl,
        Integer width,
        Integer height,
        Double duration
) {
}
