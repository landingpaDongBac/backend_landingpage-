package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LeadAdminRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^(?:\\+84|84|0)(?:3|5|7|8|9)[0-9]{8}$", message = "must be a valid Vietnamese mobile number") String phoneNumber,
        @NotBlank @Size(max = 255) String cropType,
        @Min(0) @Max(100000000) Integer treeCount,
        @Size(max = 255) String gardenArea,
        @JsonAlias("message") @Size(max = 2000) String consultationMessage,
        @NotNull LeadSource source,
        @NotNull LeadStatus status,
        @Size(max = 10000) String adminNotes,
        boolean consentAccepted,
        Instant contactedAt,
        @JsonAlias("followUpDate") Instant followUpAt
) {
}
