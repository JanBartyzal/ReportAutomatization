package com.reportplatform.admin.model.dto;

import java.util.UUID;

public record CreatePromotedTableRequest(
        String ddl,
        UUID mappingTemplateId,
        String tableName,
        int dualWriteDays) {
}
