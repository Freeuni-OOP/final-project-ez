package com.algorythm.dto;

import com.algorythm.model.Report;
import java.time.Instant;

/**
 * A report as an admin sees it in the review list: what was reported, a short
 * label for it, who reported it, why, and its status.
 */
public record ReportResponse(
        Long id,
        String targetType,
        Long targetId,
        String targetLabel,
        String reporter,
        String reason,
        String status,
        Instant createdAt) {

    private static final int LABEL_MAX = 80;

    public static ReportResponse from(Report r) {
        String targetType;
        Long targetId;
        String targetLabel;
        if (r.getComposition() != null) {
            targetType = "COMPOSITION";
            targetId = r.getComposition().getId();
            targetLabel = r.getComposition().getTitle();
        } else {
            targetType = "COMMENT";
            targetId = r.getComment().getId();
            targetLabel = snippet(r.getComment().getBody());
        }
        return new ReportResponse(
                r.getId(),
                targetType,
                targetId,
                targetLabel,
                r.getReporter().getUsername(),
                r.getReason(),
                r.getStatus().name(),
                r.getCreatedAt());
    }

    private static String snippet(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= LABEL_MAX ? body : body.substring(0, LABEL_MAX) + "…";
    }
}
