package com.algorythm.dto;

import com.algorythm.model.Composition;
import java.time.Instant;

/**
 * Public view of a composition (no owner internals — only the author's username).
 * Used by the explore feed and share links.
 */
public record PublicCompositionResponse(
        String slug,
        String title,
        String pattern,
        int bpm,
        String author,
        Instant createdAt,
        Instant updatedAt) {

    public static PublicCompositionResponse from(Composition c) {
        return new PublicCompositionResponse(
                c.getSlug(),
                c.getTitle(),
                c.getPattern(),
                c.getBpm(),
                c.getOwner().getUsername(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
