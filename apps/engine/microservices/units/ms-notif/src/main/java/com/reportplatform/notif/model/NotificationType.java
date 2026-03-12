package com.reportplatform.notif.model;

/**
 * Notification types for lifecycle events.
 */
public enum NotificationType {
    REPORT_SUBMITTED,
    REPORT_APPROVED,
    REPORT_REJECTED,
    DEADLINE_APPROACHING,
    DEADLINE_MISSED,
    FILE_PROCESSED,
    FILE_FAILED,
    BATCH_COMPLETED
}
