package com.algorythm.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * A tiny in-memory fixed-window rate limiter. Each key (e.g. a caller's IP for a
 * given bucket) gets a counter that resets once its window elapses. Good enough to
 * stop hammering without any external dependency; state is per-instance and not
 * shared across replicas.
 */
@Component
public class RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Returns true if this request is within the limit, false if it should be rejected. */
    public boolean allow(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= windowMs) {
                return new Window(now);
            }
            existing.count++;
            return existing;
        });
        return window.count <= maxRequests;
    }

    private static final class Window {
        final long start;
        int count;

        Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}
