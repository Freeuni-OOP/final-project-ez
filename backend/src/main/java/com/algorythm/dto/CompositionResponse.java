package com.algorythm.dto;

import com.algorythm.model.Composition;
import java.time.Instant;

/** Public view of a composition (no owner internals exposed). */
public record CompositionResponse(
        Long id,
        String title,
        String pattern,
        int bpm,
        Instant createdAt,
        Instant updatedAt) {

    public static CompositionResponse from(Composition c) {
        return new CompositionResponse(
                c.getId(),
                c.getTitle(),
                c.getPattern(),
                c.getBpm(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
