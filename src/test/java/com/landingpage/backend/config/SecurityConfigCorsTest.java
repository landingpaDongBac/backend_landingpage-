package com.landingpage.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null, new ObjectMapper());

    @Test
    void allowsBothLocalDevelopmentFrontendsWithoutAllowingUnknownOrigins() {
        var source = securityConfig.corsConfigurationSource(String.join(",",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174",
                "https://public-frontend.example.vercel.app"));
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(configuration.checkOrigin("http://127.0.0.1:5173")).isEqualTo("http://127.0.0.1:5173");
        assertThat(configuration.checkOrigin("http://localhost:5174")).isEqualTo("http://localhost:5174");
        assertThat(configuration.checkOrigin("http://127.0.0.1:5174")).isEqualTo("http://127.0.0.1:5174");
        assertThat(configuration.checkOrigin("https://public-frontend.example.vercel.app"))
                .isEqualTo("https://public-frontend.example.vercel.app");
        assertThat(configuration.checkOrigin("https://untrusted.example")).isNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedMethods()).contains("OPTIONS", "GET", "POST");
        assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }
}
