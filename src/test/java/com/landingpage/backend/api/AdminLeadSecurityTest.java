package com.landingpage.backend.api;

import com.landingpage.backend.config.SecurityConfig;
import com.landingpage.backend.security.JwtAuthenticationFilter;
import com.landingpage.backend.service.LeadService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLeadController.class)
@Import(SecurityConfig.class)
class AdminLeadSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockBean LeadService leadService;
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
    void anonymousUserCannotReadPrivateLeads() throws Exception {
        mockMvc.perform(get("/api/admin/leads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void editorCannotReadPrivateLeads() throws Exception {
        mockMvc.perform(get("/api/admin/leads").with(user("editor@example.com").roles("EDITOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadLeads() throws Exception {
        mockMvc.perform(get("/api/admin/leads").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
