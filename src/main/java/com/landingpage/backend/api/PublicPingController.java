package com.landingpage.backend.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/ping")
@Tag(name = "Public Ping", description = "Lightweight process-alive check")
@SecurityRequirements
public class PublicPingController {

    private static final Map<String, String> RESPONSE = Map.of(
            "status", "UP",
            "service", "agricultural-landing-backend");

    @GetMapping
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(RESPONSE);
    }
}
