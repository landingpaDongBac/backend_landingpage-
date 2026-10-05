package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landingpage.backend.domain.ContentStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentDocumentResponse(
        UUID id,
        SectionResponse section,
        String title,
        String slug,
        JsonNode contentJson,
        JsonNode publishedContentJson,
        ContentStatus status,
        int displayOrder,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant archivedAt,
        String createdBy,
        String updatedBy,
        String publishedBy
) {
}
