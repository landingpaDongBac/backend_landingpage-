package com.landingpage.backend.service;

import com.landingpage.backend.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PublicLeadRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final ConcurrentHashMap<String, Deque<Instant>> requests = new ConcurrentHashMap<>();

    public PublicLeadRateLimiter(
            @Value("${app.rate-limit.public-leads.max-requests}") int maxRequests,
            @Value("${app.rate-limit.public-leads.window-minutes}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public void check(String clientKey) {
        Instant now = Instant.now();
        Deque<Instant> clientRequests = requests.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());
        synchronized (clientRequests) {
            Instant cutoff = now.minus(window);
            while (!clientRequests.isEmpty() && clientRequests.peekFirst().isBefore(cutoff)) {
                clientRequests.removeFirst();
            }
            if (clientRequests.size() >= maxRequests) {
                throw new RateLimitExceededException("Too many lead submissions. Please try again later.");
            }
            clientRequests.addLast(now);
        }
        if (requests.size() > 10_000) {
            requests.entrySet().removeIf(entry -> {
                Deque<Instant> values = entry.getValue();
                synchronized (values) {
                    return values.isEmpty() || values.peekLast().isBefore(now.minus(window));
                }
            });
        }
    }
}
