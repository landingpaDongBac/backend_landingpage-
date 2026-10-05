package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.PublicContentResponse;
import com.landingpage.backend.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/public/content")
@RequiredArgsConstructor
@Tag(name = "Public Content", description = "Published snapshots from enabled sections only")
@SecurityRequirements
public class PublicContentController {

    private final ContentService contentService;

    @GetMapping
    public List<PublicContentResponse> list() {
        return contentService.listPublished();
    }

    @GetMapping("/{slug}")
    public PublicContentResponse get(@PathVariable String slug) {
        return contentService.getPublished(slug);
    }
}
