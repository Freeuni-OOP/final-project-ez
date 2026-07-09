package com.algorythm.dto;

import com.algorythm.model.Composition;
import com.algorythm.model.Tag;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Public view of a composition (no owner internals — only the author's username).
 * Used by the explore feed, tag/search browsing, and share links. likeCount is
 * the total likes; likedByMe is true only when the request is from a logged-in
 * user who liked it.
 */
public record PublicCompositionResponse(
        String slug,
        String title,
        String pattern,
        int bpm,
        String author,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        List<String> tags) {

    public static PublicCompositionResponse from(Composition c, long likeCount, boolean likedByMe) {
        return new PublicCompositionResponse(
                c.getSlug(),
                c.getTitle(),
                c.getPattern(),
                c.getBpm(),
                c.getOwner().getUsername(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                likeCount,
                likedByMe,
                c.getTags().stream().map(Tag::getName).sorted(Comparator.naturalOrder()).toList());
    }
}