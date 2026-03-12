package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ver.model.VersionEntity;
import com.reportplatform.ver.model.dto.CreateVersionRequest;
import com.reportplatform.ver.model.dto.VersionResponse;
import com.reportplatform.ver.repository.VersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    private final VersionRepository versionRepository;
    private final DaprEventPublisher daprEventPublisher;
    private final ObjectMapper objectMapper;

    public VersionService(VersionRepository versionRepository,
                          DaprEventPublisher daprEventPublisher,
                          ObjectMapper objectMapper) {
        this.versionRepository = versionRepository;
        this.daprEventPublisher = daprEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VersionResponse createVersion(CreateVersionRequest request, UUID orgId) {
        int nextVersion = versionRepository
                .findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(
                        request.entityType(), request.entityId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        VersionEntity entity = new VersionEntity();
        entity.setEntityType(request.entityType());
        entity.setEntityId(request.entityId());
        entity.setVersionNumber(nextVersion);
        entity.setOrgId(orgId);
        entity.setSnapshotData(request.snapshotData().toString());
        entity.setCreatedBy(request.createdBy());
        entity.setReason(request.reason());

        entity = versionRepository.save(entity);

        daprEventPublisher.publishVersionCreated(
                entity.getId(), entity.getEntityType(), entity.getEntityId(),
                nextVersion, orgId);

        log.info("Created version {} for {}/{} (org={})",
                nextVersion, request.entityType(), request.entityId(), orgId);

        return VersionResponse.from(entity);
    }

    @Transactional
    public VersionResponse createVersionOnLockedEntity(CreateVersionRequest request, UUID orgId) {
        VersionResponse version = createVersion(request, orgId);

        daprEventPublisher.publishEditOnLocked(
                request.entityType(), request.entityId(), version.versionNumber(), orgId);

        log.info("Edit on locked entity {}/{} → new version {}, report returns to DRAFT",
                request.entityType(), request.entityId(), version.versionNumber());

        return version;
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> listVersions(String entityType, UUID entityId) {
        return versionRepository
                .findByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId)
                .stream()
                .map(VersionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VersionEntity getVersionEntity(String entityType, UUID entityId, int versionNumber) {
        return versionRepository
                .findByEntityTypeAndEntityIdAndVersionNumber(entityType, entityId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version %d not found for %s/%s".formatted(versionNumber, entityType, entityId)));
    }

    @Transactional
    public void lockVersions(String entityType, UUID entityId) {
        int updated = versionRepository.lockAllVersions(entityType, entityId);
        log.info("Locked {} versions for {}/{}", updated, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public boolean isLatestVersionLocked(String entityType, UUID entityId) {
        return versionRepository
                .findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId)
                .map(VersionEntity::getLocked)
                .orElse(false);
    }
}
