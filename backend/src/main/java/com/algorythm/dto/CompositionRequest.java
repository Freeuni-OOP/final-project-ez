package com.algorythm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating/updating a composition. */
public record CompositionRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10000) String pattern,
        @Min(20) @Max(300) int bpm) {
}
