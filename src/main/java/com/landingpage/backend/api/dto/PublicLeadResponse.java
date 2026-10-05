package com.landingpage.backend.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicLeadResponse(UUID id, String message, Instant createdAt) {
}
