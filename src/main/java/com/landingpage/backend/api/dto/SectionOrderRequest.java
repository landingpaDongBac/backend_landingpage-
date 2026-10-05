package com.landingpage.backend.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SectionOrderRequest(@NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull UUID sectionId, @Min(0) int displayOrder) { }
}
