package com.algorythm.model;

/** Where a report is in the review process. */
public enum ReportStatus {
    /** Awaiting an admin's review. */
    OPEN,
    /** An admin has dealt with it (dismissed, or the content was removed). */
    RESOLVED
}
