package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.LeadAdminRequest;
import com.landingpage.backend.api.dto.LeadResponse;
import com.landingpage.backend.api.dto.LeadStatusUpdateRequest;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/leads")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Leads", description = "ADMIN-only consultation lead management")
public class AdminLeadController {

    private final LeadService leadService;

    @GetMapping
    public Page<LeadResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return leadService.search(q, crop, status, source, deleted, createdFrom, createdTo, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public LeadResponse get(@PathVariable UUID id) {
        return leadService.get(id);
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createAdmin(request));
    }

    @PutMapping("/{id}")
    public LeadResponse update(@PathVariable UUID id, @Valid @RequestBody LeadAdminRequest request) {
        return leadService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public LeadResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody LeadStatusUpdateRequest request) {
        return leadService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public LeadResponse restore(@PathVariable UUID id) {
        return leadService.restore(id);
    }
}
