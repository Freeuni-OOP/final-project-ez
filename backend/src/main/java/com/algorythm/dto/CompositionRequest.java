package com.algorythm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Body for creating/updating a composition. tags is optional (defaulted to
 * empty by the controller when omitted); each name is normalized and at most
 * 5 distinct tags are kept - see TagService.
 */
public record CompositionRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10000) String pattern,
        @Min(20) @Max(300) int bpm,
        List<String> tags) {
}