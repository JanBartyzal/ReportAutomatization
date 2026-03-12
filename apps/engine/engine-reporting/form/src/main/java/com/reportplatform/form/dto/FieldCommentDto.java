package com.reportplatform.form.dto;

import com.reportplatform.form.model.FormFieldCommentEntity;

import java.time.Instant;
import java.util.UUID;

public record FieldCommentDto(
        UUID id,
        String fieldKey,
        String comment,
        String userId,
        Instant createdAt
) {
    public static FieldCommentDto from(FormFieldCommentEntity entity) {
        return new FieldCommentDto(
                entity.getId(),
                entity.getFieldKey(),
                entity.getComment(),
                entity.getUserId(),
                entity.getCreatedAt()
        );
    }
}
