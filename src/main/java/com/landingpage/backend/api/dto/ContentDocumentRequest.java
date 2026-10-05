package com.landingpage.backend.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ContentDocumentRequest(
        @NotNull UUID sectionId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase URL slug") String slug,
        @NotNull JsonNode contentJson,
        @Min(0) int displayOrder,
        @PositiveOrZero Long version
) {
}
