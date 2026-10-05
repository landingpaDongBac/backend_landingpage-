package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        String cropType,
        Integer treeCount,
        String gardenArea,
        String consultationMessage,
        LeadSource source,
        LeadStatus status,
        String adminNotes,
        boolean consentAccepted,
        Instant createdAt,
        Instant updatedAt,
        Instant contactedAt,
        Instant followUpAt,
        boolean deleted,
        Instant deletedAt
) {
}
