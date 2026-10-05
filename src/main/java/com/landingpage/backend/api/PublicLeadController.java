package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.PublicLeadRequest;
import com.landingpage.backend.api.dto.PublicLeadResponse;
import com.landingpage.backend.service.LeadService;
import com.landingpage.backend.service.PublicLeadRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/public/leads")
@RequiredArgsConstructor
@Tag(name = "Public Leads", description = "Public consultation submission with validation, consent, spam protection, and rate limiting")
@SecurityRequirements
public class PublicLeadController {

    private final LeadService leadService;
    private final PublicLeadRateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<PublicLeadResponse> create(@Valid @RequestBody PublicLeadRequest request,
                                                     HttpServletRequest servletRequest) {
        rateLimiter.check(servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createPublic(request));
    }
}
