package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;

import java.time.Instant;

public record SystemSettingsResponse(
        String websiteName,
        String defaultLanguage,
        String publicInformation,
        String supportPhone,
        String contactEmail,
        String address,
        String supportHours,
        String zaloUrl,
        String facebookUrl,
        LeadStatus defaultLeadStatus,
        LeadSource defaultLeadSource,
        boolean publicationApprovalRequired,
        int previewWidth,
        JsonNode displayConfiguration,
        InfrastructureStatus infrastructure,
        Instant updatedAt,
        String updatedBy
) {
    public record InfrastructureStatus(boolean databaseConnected, boolean cloudinaryConfigured,
                                       boolean directUploadAvailable) { }
}
