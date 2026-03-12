package com.reportplatform.audit.service;

import com.reportplatform.audit.model.AiAuditLogEntity;
import com.reportplatform.audit.model.dto.AiAuditLogResponse;
import com.reportplatform.audit.repository.AiAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AiAuditLogService.class);

    private final AiAuditLogRepository aiAuditLogRepository;

    public AiAuditLogService(AiAuditLogRepository aiAuditLogRepository) {
        this.aiAuditLogRepository = aiAuditLogRepository;
    }

    @Transactional
    public void logAiInteraction(UUID orgId, String userId, String promptText,
                                  String responseText, String model, Integer tokensUsed) {
        AiAuditLogEntity entity = new AiAuditLogEntity();
        entity.setOrgId(orgId);
        entity.setUserId(userId);
        entity.setPromptText(promptText);
        entity.setResponseText(responseText);
        entity.setModel(model);
        entity.setTokensUsed(tokensUsed);

        aiAuditLogRepository.save(entity);

        log.debug("AI audit logged: user={} model={} tokens={}", userId, model, tokensUsed);
    }

    @Transactional(readOnly = true)
    public Page<AiAuditLogResponse> getAiAuditHistory(UUID orgId, Pageable pageable) {
        return aiAuditLogRepository
                .findByOrgIdOrderByCreatedAtDesc(orgId, pageable)
                .map(AiAuditLogResponse::from);
    }
}
