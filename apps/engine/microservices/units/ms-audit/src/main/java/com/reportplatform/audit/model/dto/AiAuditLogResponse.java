package com.reportplatform.audit.model.dto;

import com.reportplatform.audit.model.AiAuditLogEntity;

import java.time.Instant;
import java.util.UUID;

public record AiAuditLogResponse(
        UUID id,
        String userId,
        String promptText,
        String model,
        Integer tokensUsed,
        Instant createdAt
) {
    public static AiAuditLogResponse from(AiAuditLogEntity entity) {
        String truncatedPrompt = entity.getPromptText();
        if (truncatedPrompt != null && truncatedPrompt.length() > 200) {
            truncatedPrompt = truncatedPrompt.substring(0, 200) + "...";
        }
        return new AiAuditLogResponse(
                entity.getId(),
                entity.getUserId(),
                truncatedPrompt,
                entity.getModel(),
                entity.getTokensUsed(),
                entity.getCreatedAt()
        );
    }
}
