package com.algorythm.dto;

import com.algorythm.model.Comment;
import java.time.Instant;

/**
 * Public view of a comment: the author's username only (never the email) plus
 * the text and when it was posted.
 */
public record CommentResponse(
        Long id,
        String author,
        String body,
        Instant createdAt) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getAuthor().getUsername(),
                c.getBody(),
                c.getCreatedAt());
    }
}
