package com.reportplatform.admin.model.dto;

import java.time.Instant;
import java.util.UUID;

public record RoleAssignmentResponse(
        String targetUserId,
        UUID orgId,
        String role,
        String assignedBy,
        Instant assignedAt
) {
}
