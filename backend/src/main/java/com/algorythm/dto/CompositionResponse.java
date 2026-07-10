package com.algorythm.dto;

import com.algorythm.model.Composition;
import com.algorythm.model.Tag;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Owner's view of a composition, including its publish state + share slug. */
public record CompositionResponse(
        Long id,
        String title,
        String pattern,
        int bpm,
        boolean isPublic,
        String slug,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags,
        Long remixedFromId,
        String remixedFromTitle,
        String remixedFromAuthor) {

    public static CompositionResponse from(Composition c) {
        var source = c.getRemixedFrom();
        return new CompositionResponse(
                c.getId(),
                c.getTitle(),
                c.getPattern(),
                c.getBpm(),
                c.isPublic(),
                c.getSlug(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getTags().stream().map(Tag::getName).sorted(Comparator.naturalOrder()).toList(),
                source == null ? null : source.getId(),
                source == null ? null : source.getTitle(),
                source == null ? null : source.getOwner().getUsername());
    }
}
