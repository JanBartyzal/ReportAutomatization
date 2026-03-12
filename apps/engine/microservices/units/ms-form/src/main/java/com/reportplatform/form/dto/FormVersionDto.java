package com.reportplatform.form.dto;

import com.reportplatform.form.model.FormVersionEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FormVersionDto(
        UUID id,
        UUID formId,
        int versionNumber,
        Map<String, Object> schemaDef,
        String createdBy,
        Instant createdAt
) {
    public static FormVersionDto from(FormVersionEntity entity) {
        return new FormVersionDto(
                entity.getId(),
                entity.getFormId(),
                entity.getVersionNumber(),
                entity.getSchemaDef(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
