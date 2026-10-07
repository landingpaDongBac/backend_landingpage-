package com.landingpage.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null, new ObjectMapper());

    @Test
    void allowsLocalAdminOldPublicAndCustomDomainsWithoutAllowingUnknownOrigins() {
        var source = securityConfig.corsConfigurationSource(String.join(",",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174",
                "https://fe-admin-landingpage-ivory.vercel.app",
                "https://landingpage-fe-three.vercel.app",
                "https://vtnnchogiong.io.vn",
                "https://www.vtnnchogiong.io.vn"));
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(configuration.checkOrigin("http://127.0.0.1:5173")).isEqualTo("http://127.0.0.1:5173");
        assertThat(configuration.checkOrigin("http://localhost:5174")).isEqualTo("http://localhost:5174");
        assertThat(configuration.checkOrigin("http://127.0.0.1:5174")).isEqualTo("http://127.0.0.1:5174");
        assertThat(configuration.checkOrigin("https://landingpage-fe-three.vercel.app"))
                .isEqualTo("https://landingpage-fe-three.vercel.app");
        assertThat(configuration.checkOrigin("https://vtnnchogiong.io.vn"))
                .isEqualTo("https://vtnnchogiong.io.vn");
        assertThat(configuration.checkOrigin("https://www.vtnnchogiong.io.vn"))
                .isEqualTo("https://www.vtnnchogiong.io.vn");
        assertThat(configuration.checkOrigin("https://fe-admin-landingpage-ivory.vercel.app"))
                .isEqualTo("https://fe-admin-landingpage-ivory.vercel.app");
        assertThat(configuration.checkOrigin("https://untrusted.example")).isNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedMethods()).contains("OPTIONS", "GET", "POST");
        assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }
}
