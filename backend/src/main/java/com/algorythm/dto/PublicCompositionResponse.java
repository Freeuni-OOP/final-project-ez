package com.algorythm.dto;

import com.algorythm.model.Composition;
import java.time.Instant;

/**
 * Public view of a composition (no owner internals — only the author's username).
 * Used by the explore feed and share links. id is exposed so authenticated
 * clients can call the like endpoints. likeCount is the total likes; likedByMe is
 * true only when the request is from a logged-in user who liked it.
 */
public record PublicCompositionResponse(
        Long id,
        String slug,
        String title,
        String pattern,
        int bpm,
        String author,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe) {

    public static PublicCompositionResponse from(Composition c, long likeCount, boolean likedByMe) {
        return new PublicCompositionResponse(
                c.getId(),
                c.getSlug(),
                c.getTitle(),
                c.getPattern(),
                c.getBpm(),
                c.getOwner().getUsername(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                likeCount,
                likedByMe);
    }
}
