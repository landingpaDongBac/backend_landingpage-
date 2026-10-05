package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record PublicContentResponse(
        UUID id,
        String sectionKey,
        String sectionName,
        int sectionOrder,
        String title,
        String slug,
        JsonNode contentJson,
        List<PublicMediaResponse> media,
        int displayOrder,
        Instant publishedAt
) {
}
