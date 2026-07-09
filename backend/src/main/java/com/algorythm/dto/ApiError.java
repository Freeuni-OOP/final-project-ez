package com.algorythm.dto;

import java.time.Instant;

/**
 * The single error shape every failed API request returns, so the frontend can
 * always read the same fields. The message is safe to show to a user; no stack
 * traces or internal details are ever included.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
