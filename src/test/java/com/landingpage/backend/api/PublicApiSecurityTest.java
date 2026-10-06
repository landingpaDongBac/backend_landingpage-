package com.landingpage.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.landingpage.backend.api.dto.PublicLeadResponse;
import com.landingpage.backend.api.dto.PublicSiteSettingsResponse;
import com.landingpage.backend.config.SecurityConfig;
import com.landingpage.backend.security.JwtAuthenticationFilter;
import com.landingpage.backend.service.ContentService;
import com.landingpage.backend.service.LeadService;
import com.landingpage.backend.service.PublicLeadRateLimiter;
import com.landingpage.backend.service.SystemSettingsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PublicPingController.class, PublicContentController.class,
        PublicSiteSettingsController.class, PublicLeadController.class})
@Import(SecurityConfig.class)
class PublicApiSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ContentService contentService;
    @MockBean SystemSettingsService settingsService;
    @MockBean LeadService leadService;
    @MockBean PublicLeadRateLimiter rateLimiter;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsService userDetailsService;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousPingIsLightweightAndReturnsServiceIdentity() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("agricultural-landing-backend"));
    }

    @Test
    void anonymousPublicContentAndSettingsRemainAccessible() throws Exception {
        when(contentService.listPublished()).thenReturn(List.of());
        when(settingsService.getPublic()).thenReturn(new PublicSiteSettingsResponse(
                "Agricultural Landing", null, null, null, null,
                "https://zalo.me/0901234567",
                JsonNodeFactory.instance.objectNode()));

        mockMvc.perform(get("/api/public/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/public/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.websiteName").value("Agricultural Landing"))
                .andExpect(jsonPath("$.zaloUrl").value("https://zalo.me/0901234567"));
    }

    @Test
    void anonymousValidatedLeadSubmissionRemainsAccessible() throws Exception {
        UUID id = UUID.randomUUID();
        when(leadService.createPublic(any())).thenReturn(
                new PublicLeadResponse(id, "Consultation request received", Instant.now()));

        mockMvc.perform(post("/api/public/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new java.util.LinkedHashMap<>(java.util.Map.of(
                                "fullName", "Nguyen Van A",
                                "phoneNumber", "0901234567",
                                "cropType", "Coffee",
                                "source", "CONSULTATION_FORM",
                                "consentAccepted", true)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/public/leads")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/admin/settings")
                        .header("Origin", "https://fe-admin-landingpage-ivory.vercel.app")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://fe-admin-landingpage-ivory.vercel.app"));
    }
}
