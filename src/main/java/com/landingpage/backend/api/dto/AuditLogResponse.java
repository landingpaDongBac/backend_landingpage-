package com.landingpage.backend.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String action,
        String entityType,
        String entityId,
        String description,
        Instant createdAt,
        String ipAddress
) {
}
