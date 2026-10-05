package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.SystemSettingsRequest;
import com.landingpage.backend.api.dto.SystemSettingsResponse;
import com.landingpage.backend.security.UserPrincipal;
import com.landingpage.backend.service.SystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "ADMIN-only typed settings and safe infrastructure status")
public class AdminSettingsController {
    private final SystemSettingsService service;

    @GetMapping
    public SystemSettingsResponse get() { return service.get(); }

    @PatchMapping
    public SystemSettingsResponse update(@Valid @RequestBody SystemSettingsRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(request, principal.email());
    }
}
