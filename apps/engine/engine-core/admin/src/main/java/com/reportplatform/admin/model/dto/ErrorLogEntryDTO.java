package com.reportplatform.admin.model.dto;

/**
 * Recent error log entry, matching frontend ErrorLogEntry interface.
 */
public record ErrorLogEntryDTO(
        String id,
        String timestamp,
        String service,
        String level,
        String message,
        String details
) {}
