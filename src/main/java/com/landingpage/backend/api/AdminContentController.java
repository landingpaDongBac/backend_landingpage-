package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.ContentDocumentRequest;
import com.landingpage.backend.api.dto.ContentDocumentResponse;
import com.landingpage.backend.api.dto.ContentOrderRequest;
import com.landingpage.backend.api.dto.ContentRevisionResponse;
import com.landingpage.backend.security.UserPrincipal;
import com.landingpage.backend.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import com.landingpage.backend.domain.ContentStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/content")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
@RequiredArgsConstructor
@Tag(name = "Admin Content", description = "ADMIN and EDITOR draft CMS; publish, unpublish, archive, and archive restore require ADMIN")
public class AdminContentController {

    private final ContentService contentService;

    @GetMapping
    public List<ContentDocumentResponse> list() {
        return contentService.listAdmin();
    }

    @GetMapping("/page")
    public Page<ContentDocumentResponse> page(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return contentService.searchAdmin(q, sectionId, status, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public ContentDocumentResponse get(@PathVariable UUID id) {
        return contentService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<ContentDocumentResponse> create(
            @Valid @RequestBody ContentDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.create(request, principal.email()));
    }

    @PutMapping("/{id}")
    public ContentDocumentResponse update(@PathVariable UUID id,
                                          @Valid @RequestBody ContentDocumentRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.update(id, request, principal.email());
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDocumentResponse publish(@PathVariable UUID id,
                                           @RequestHeader(value = "If-Match", required = false) Long expectedVersion,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.publish(id, expectedVersion, principal.email());
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDocumentResponse unpublish(@PathVariable UUID id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.unpublish(id, principal.email());
    }

    @PatchMapping("/{id}/order")
    public ContentDocumentResponse reorder(@PathVariable UUID id,
                                           @Valid @RequestBody ContentOrderRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.reorder(id, request.displayOrder(), principal.email());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDocumentResponse archive(@PathVariable UUID id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.archive(id, principal.email());
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDocumentResponse restoreArchived(@PathVariable UUID id,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.restoreArchived(id, principal.email());
    }

    @GetMapping("/{id}/revisions")
    public List<ContentRevisionResponse> revisions(@PathVariable UUID id) {
        return contentService.revisions(id);
    }

    @GetMapping("/{id}/revisions/{revisionId}")
    public ContentRevisionResponse revision(@PathVariable UUID id, @PathVariable UUID revisionId) {
        return contentService.revision(id, revisionId);
    }

    @PostMapping("/{id}/revisions/{revisionId}/restore")
    public ContentDocumentResponse restoreRevision(@PathVariable UUID id, @PathVariable UUID revisionId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.restoreRevision(id, revisionId, principal.email());
    }
}
