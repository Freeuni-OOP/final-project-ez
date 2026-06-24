package com.algorythm.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint used to confirm the backend is up.
 *
 * <p>All backend routes live under the {@code /api} prefix so the frontend wiring
 * (proxy / CORS) stays simple. This one returns a small JSON body; later issues add
 * a database check here once Postgres is wired in.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "algorythm-backend",
                "time", Instant.now().toString()
        );
    }
}
