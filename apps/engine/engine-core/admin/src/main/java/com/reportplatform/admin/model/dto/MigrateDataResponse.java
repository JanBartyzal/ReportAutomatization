package com.reportplatform.admin.model.dto;

public record MigrateDataResponse(
        String migrationId,
        long recordsMigrated,
        String message) {
}
