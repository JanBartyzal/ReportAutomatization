package com.reportplatform.admin.model.dto;

import java.util.UUID;

public record MigrateDataRequest(
        UUID mappingTemplateId,
        String tableName) {
}
