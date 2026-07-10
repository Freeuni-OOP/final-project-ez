package com.algorythm.dto;

import jakarta.validation.constraints.Size;

/** Body for reporting content. The reason is optional. */
public record ReportRequest(
        @Size(max = 1000) String reason) {
}
