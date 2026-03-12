package com.reportplatform.form.dto;

import com.reportplatform.form.model.FormAssignmentEntity;

import java.time.Instant;
import java.util.UUID;

public record FormAssignmentDto(
        UUID id,
        UUID formId,
        String orgId,
        String status,
        String assignedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static FormAssignmentDto from(FormAssignmentEntity entity) {
        return new FormAssignmentDto(
                entity.getId(),
                entity.getFormId(),
                entity.getOrgId(),
                entity.getStatus(),
                entity.getAssignedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
