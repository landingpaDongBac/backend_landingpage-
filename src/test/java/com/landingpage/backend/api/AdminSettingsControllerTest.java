package com.landingpage.backend.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.landingpage.backend.api.dto.SystemSettingsResponse;
import com.landingpage.backend.config.SecurityConfig;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.security.JwtAuthenticationFilter;
import com.landingpage.backend.service.SystemSettingsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSettingsController.class)
@Import(SecurityConfig.class)
class AdminSettingsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean SystemSettingsService settingsService;
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
    @WithMockUser(roles = "ADMIN")
    void getReturnsAllContactSettings() throws Exception {
        when(settingsService.get()).thenReturn(new SystemSettingsResponse(
                "Agricultural Landing", "vi", null,
                "0901234567", "support@example.com", "Dak Lak",
                "Thứ Hai - Thứ Bảy, 08:00 - 17:00",
                "https://zalo.me/0901234567",
                "https://www.facebook.com/agricultural-landing",
                LeadStatus.NEW, LeadSource.OTHER, true, 1200,
                JsonNodeFactory.instance.objectNode(),
                new SystemSettingsResponse.InfrastructureStatus(true, true, true),
                Instant.parse("2026-10-07T00:00:00Z"), "admin@example.com"));

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportPhone").value("0901234567"))
                .andExpect(jsonPath("$.contactEmail").value("support@example.com"))
                .andExpect(jsonPath("$.address").value("Dak Lak"))
                .andExpect(jsonPath("$.supportHours").value("Thứ Hai - Thứ Bảy, 08:00 - 17:00"))
                .andExpect(jsonPath("$.zaloUrl").value("https://zalo.me/0901234567"))
                .andExpect(jsonPath("$.facebookUrl").value("https://www.facebook.com/agricultural-landing"));
    }
}
