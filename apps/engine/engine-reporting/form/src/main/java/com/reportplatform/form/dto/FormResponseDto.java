package com.reportplatform.form.dto;

import com.reportplatform.form.model.FormResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FormResponseDto(
        UUID id,
        UUID formId,
        UUID formVersionId,
        String orgId,
        UUID periodId,
        String userId,
        String status,
        Map<String, Object> data,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        List<FieldCommentDto> comments
) {
    public static FormResponseDto from(FormResponseEntity entity, List<FieldCommentDto> comments) {
        return new FormResponseDto(
                entity.getId(),
                entity.getFormId(),
                entity.getFormVersionId(),
                entity.getOrgId(),
                entity.getPeriodId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getData(),
                entity.getSubmittedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                comments
        );
    }

    public static FormResponseDto from(FormResponseEntity entity) {
        return from(entity, null);
    }
}
