package com.algorythm.dto;

/** Basic site numbers for the admin dashboard. */
public record SiteStatsResponse(
        long userCount,
        long compositionCount,
        long openReportCount) {
}
