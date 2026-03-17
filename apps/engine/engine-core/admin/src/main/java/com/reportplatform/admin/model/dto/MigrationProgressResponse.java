package com.reportplatform.admin.model.dto;

public record MigrationProgressResponse(
        String status,
        long recordsMigrated,
        long totalRecords,
        int progressPercent) {
}
