package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.PublicSiteSettingsResponse;
import com.landingpage.backend.service.SystemSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/site-settings")
@RequiredArgsConstructor
@Tag(name = "Public Site Settings", description = "Approved public website and contact information")
@SecurityRequirements
public class PublicSiteSettingsController {

    private final SystemSettingsService settingsService;

    @GetMapping
    public PublicSiteSettingsResponse get() {
        return settingsService.getPublic();
    }
}
