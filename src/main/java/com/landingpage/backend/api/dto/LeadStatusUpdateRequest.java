package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.landingpage.backend.domain.LeadStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LeadStatusUpdateRequest(
        @NotNull LeadStatus status,
        @Size(max = 10000) String adminNotes,
        Instant contactedAt,
        @JsonAlias("followUpDate") Instant followUpAt
) {
}
