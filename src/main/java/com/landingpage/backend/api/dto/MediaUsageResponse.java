package com.landingpage.backend.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MediaUsageResponse(UUID mediaId, boolean inUse, List<Usage> usages) {
    public record Usage(String usageType, UUID documentId, String documentTitle,
                        UUID revisionId, Integer revisionNumber, Instant createdAt) { }
}
