package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SystemSettingsRequest(
        @Size(max = 255) String websiteName,
        @Pattern(regexp = "^[a-z]{2}(?:-[A-Z]{2})?$", message = "must be a language code such as vi or vi-VN") String defaultLanguage,
        @Size(max = 5000) String publicInformation,
        @Size(max = 50) String supportPhone,
        @Email @Size(max = 320) String contactEmail,
        @Size(max = 1000) String address,
        @Size(max = 500) String zaloUrl,
        LeadStatus defaultLeadStatus,
        LeadSource defaultLeadSource,
        Boolean publicationApprovalRequired,
        @Min(320) @Max(3840) Integer previewWidth,
        JsonNode displayConfiguration
) {
}
