package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ContentRevisionResponse(
        UUID id,
        UUID documentId,
        String documentTitle,
        int versionNumber,
        JsonNode contentJson,
        Instant createdAt,
        String createdBy,
        String changeReason
) {
}
