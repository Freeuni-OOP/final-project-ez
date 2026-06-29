package com.algorythm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for posting a comment. */
public record CommentRequest(
        @NotBlank @Size(max = 2000) String body) {
}
