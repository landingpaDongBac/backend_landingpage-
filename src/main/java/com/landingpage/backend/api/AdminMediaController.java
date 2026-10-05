package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.MediaAssetResponse;
import com.landingpage.backend.api.dto.MediaConfirmRequest;
import com.landingpage.backend.api.dto.UploadSignatureRequest;
import com.landingpage.backend.api.dto.UploadSignatureResponse;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.security.UserPrincipal;
import com.landingpage.backend.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import com.landingpage.backend.api.dto.MediaUsageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
@RequiredArgsConstructor
@Tag(name = "Admin Media", description = "ADMIN and EDITOR Cloudinary asset management")
public class AdminMediaController {

    private final MediaService mediaService;

    @GetMapping
    public Page<MediaAssetResponse> list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) MediaResourceType resourceType,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(defaultValue = "createdAt") String sortBy,
                                         @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return mediaService.list(q, resourceType, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public MediaAssetResponse get(@PathVariable UUID id) {
        return mediaService.get(id);
    }

    @GetMapping("/{id}/usages")
    public MediaUsageResponse usages(@PathVariable UUID id) {
        return mediaService.usages(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam MediaResourceType resourceType,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.upload(file, resourceType, principal.email()));
    }

    @PostMapping("/upload-signature")
    public UploadSignatureResponse signature(@Valid @RequestBody UploadSignatureRequest request) {
        return mediaService.createUploadSignature(request.resourceType());
    }

    @PostMapping("/confirm")
    public ResponseEntity<MediaAssetResponse> confirm(@Valid @RequestBody MediaConfirmRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.confirmDirectUpload(request, principal.email()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
