package com.landingpage.backend.api.dto;

import jakarta.validation.constraints.Min;

public record ContentOrderRequest(@Min(0) int displayOrder) {
}
