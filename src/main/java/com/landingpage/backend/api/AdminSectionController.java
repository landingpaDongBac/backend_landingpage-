package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.SectionResponse;
import com.landingpage.backend.api.dto.SectionVisibilityRequest;
import com.landingpage.backend.api.dto.SectionOrderRequest;
import com.landingpage.backend.api.dto.SectionUpdateRequest;
import com.landingpage.backend.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/sections")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
@RequiredArgsConstructor
@Tag(name = "Admin Sections", description = "ADMIN and EDITOR reads; mutations require ADMIN")
public class AdminSectionController {

    private final SectionService sectionService;

    @GetMapping
    public List<SectionResponse> list() {
        return sectionService.list();
    }

    @GetMapping("/{id}")
    public SectionResponse get(@PathVariable UUID id) {
        return sectionService.get(id);
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public SectionResponse setVisibility(@PathVariable UUID id,
                                         @Valid @RequestBody SectionVisibilityRequest request) {
        return sectionService.setVisibility(id, request.enabled());
    }

    @PatchMapping("/order")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SectionResponse> reorder(@Valid @RequestBody SectionOrderRequest request) {
        return sectionService.reorder(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SectionResponse update(@PathVariable UUID id, @Valid @RequestBody SectionUpdateRequest request) {
        return sectionService.update(id, request);
    }
}
