package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PublicSiteSettingsResponse(
        String websiteName,
        String publicInformation,
        String supportPhone,
        String contactEmail,
        String address,
        String supportHours,
        String zaloUrl,
        String facebookUrl,
        JsonNode displayConfiguration
) {
}
