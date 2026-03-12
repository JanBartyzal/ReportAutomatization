package com.reportplatform.audit.service;

import com.reportplatform.audit.model.ReadAccessLogEntity;
import com.reportplatform.audit.model.dto.ReadAccessLogResponse;
import com.reportplatform.audit.repository.ReadAccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReadAccessLogService {

    private static final Logger log = LoggerFactory.getLogger(ReadAccessLogService.class);

    private final ReadAccessLogRepository readAccessLogRepository;

    public ReadAccessLogService(ReadAccessLogRepository readAccessLogRepository) {
        this.readAccessLogRepository = readAccessLogRepository;
    }

    @Transactional
    public void logAccess(UUID orgId, String userId, UUID documentId,
                          String ipAddress, String userAgent) {
        ReadAccessLogEntity entity = new ReadAccessLogEntity();
        entity.setOrgId(orgId);
        entity.setUserId(userId);
        entity.setDocumentId(documentId);
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(userAgent);

        readAccessLogRepository.save(entity);

        log.debug("Read access logged: user={} document={}", userId, documentId);
    }

    @Transactional(readOnly = true)
    public Page<ReadAccessLogResponse> getAccessHistory(UUID documentId, Pageable pageable) {
        return readAccessLogRepository
                .findByDocumentIdOrderByCreatedAtDesc(documentId, pageable)
                .map(ReadAccessLogResponse::from);
    }
}
