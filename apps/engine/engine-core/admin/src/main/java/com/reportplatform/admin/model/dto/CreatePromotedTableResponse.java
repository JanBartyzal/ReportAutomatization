package com.reportplatform.admin.model.dto;

public record CreatePromotedTableResponse(
        boolean success,
        String tableName,
        String message) {
}
