package com.landingpage.backend.security;

import com.landingpage.backend.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            mock(JwtService.class), mock(CustomUserDetailsService.class), mock(RefreshTokenService.class));

    @Test
    void skipsAuthenticationAndDatabaseBackedUserLookupForPublicRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/ping");
        request.setServletPath("/api/public/ping");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void stillProcessesAuthenticationForAdminRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/leads");
        request.setServletPath("/api/admin/leads");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}
