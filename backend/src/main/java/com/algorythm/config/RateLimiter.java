package com.algorythm.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * A tiny in-memory fixed-window rate limiter. Each key (e.g. a caller's IP for a
 * given bucket) gets a counter that resets once its window elapses. Expired windows
 * are swept out periodically so the map stays bounded and does not leak memory over
 * time. State is per-instance and not shared across replicas.
 */
@Component
public class RateLimiter {

    // Roughly every this many calls, drop windows whose time is up. Keeps the map
    // bounded without needing a scheduler; if there is no traffic there is nothing
    // to clean up anyway.
    private static final int SWEEP_EVERY = 1000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger callsSinceSweep = new AtomicInteger();

    /** Returns true if this request is within the limit, false if it should be rejected. */
    public boolean allow(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now >= existing.expiresAt) {
                return new Window(now + windowMs);
            }
            existing.count++;
            return existing;
        });
        maybeSweep(now);
        return window.count <= maxRequests;
    }

    /** Occasionally remove windows that have already expired so the map cannot grow forever. */
    private void maybeSweep(long now) {
        if (callsSinceSweep.incrementAndGet() < SWEEP_EVERY) {
            return;
        }
        callsSinceSweep.set(0);
        windows.values().removeIf(w -> now >= w.expiresAt);
    }

    private static final class Window {
        final long expiresAt;
        int count;

        Window(long expiresAt) {
            this.expiresAt = expiresAt;
            this.count = 1;
        }
    }
}
