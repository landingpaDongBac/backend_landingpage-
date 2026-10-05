package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.ContentRevisionResponse;
import com.landingpage.backend.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/revisions")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
@RequiredArgsConstructor
@Tag(name = "Admin Revisions", description = "ADMIN and EDITOR global content revision history")
public class AdminRevisionController {
    private final ContentService contentService;

    @GetMapping
    public Page<ContentRevisionResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID documentId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String changeReason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contentService.searchRevisions(q, documentId, createdBy, changeReason,
                createdFrom, createdTo, page, size);
    }
}
