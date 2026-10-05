package com.landingpage.backend.api.dto;

import java.util.List;

public record UploadSignatureResponse(
        long timestamp,
        String signature,
        String apiKey,
        String cloudName,
        String folder,
        String resourceType,
        List<String> allowedFormats,
        boolean useFilename,
        boolean uniqueFilename,
        boolean overwrite
) {
}
