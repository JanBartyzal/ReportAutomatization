package com.reportplatform.audit.service;

import com.reportplatform.audit.model.AuditLogEntity;
import com.reportplatform.audit.model.dto.AuditFilterRequest;
import com.reportplatform.audit.model.dto.AuditLogResponse;
import com.reportplatform.audit.model.dto.CreateAuditLogRequest;
import com.reportplatform.audit.repository.AuditLogRepository;
import com.reportplatform.audit.repository.AuditLogSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLogResponse createAuditLog(CreateAuditLogRequest request) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOrgId(request.orgId());
        entity.setUserId(request.userId());
        entity.setAction(request.action());
        entity.setEntityType(request.entityType());
        entity.setEntityId(request.entityId());
        entity.setDetails(request.details() != null ? request.details().toString() : null);
        entity.setIpAddress(request.ipAddress());
        entity.setUserAgent(request.userAgent());

        entity = auditLogRepository.save(entity);

        log.debug("Audit log created: {} by {} on {}/{}",
                request.action(), request.userId(), request.entityType(), request.entityId());

        return AuditLogResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> queryLogs(UUID orgId, AuditFilterRequest filter, Pageable pageable) {
        var spec = AuditLogSpecification.withFilters(
                orgId,
                filter.userId(),
                filter.action(),
                filter.entityType(),
                filter.entityId(),
                filter.dateFrom(),
                filter.dateTo()
        );

        return auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from);
    }
}
