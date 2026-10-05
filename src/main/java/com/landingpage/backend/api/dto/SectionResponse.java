package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record SectionResponse(
        UUID id,
        String sectionKey,
        String name,
        int displayOrder,
        boolean enabled,
        Instant updatedAt,
        long documentCount,
        String description,
        JsonNode configuration
) {
}
