package com.landingpage.backend.service;

import com.landingpage.backend.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicLeadRateLimiterTest {

    @Test
    void limitsRepeatedSubmissionsPerClient() {
        PublicLeadRateLimiter limiter = new PublicLeadRateLimiter(2, 10);

        limiter.check("127.0.0.1");
        limiter.check("127.0.0.1");

        assertThatThrownBy(() -> limiter.check("127.0.0.1"))
                .isInstanceOf(RateLimitExceededException.class);
        limiter.check("127.0.0.2");
    }
}
