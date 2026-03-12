package com.reportplatform.ver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ver.model.VersionDiffEntity;
import com.reportplatform.ver.model.VersionEntity;
import com.reportplatform.ver.model.dto.FieldChange;
import com.reportplatform.ver.model.dto.VersionDiffResponse;
import com.reportplatform.ver.repository.VersionDiffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VersionDiffService {

    private static final Logger log = LoggerFactory.getLogger(VersionDiffService.class);

    private final VersionService versionService;
    private final VersionDiffRepository diffRepository;
    private final DiffEngine diffEngine;
    private final ObjectMapper objectMapper;

    public VersionDiffService(VersionService versionService,
                              VersionDiffRepository diffRepository,
                              DiffEngine diffEngine,
                              ObjectMapper objectMapper) {
        this.versionService = versionService;
        this.diffRepository = diffRepository;
        this.diffEngine = diffEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VersionDiffResponse getDiff(String entityType, UUID entityId, int v1, int v2) {
        // Check cache first
        var cached = diffRepository.findByEntityTypeAndEntityIdAndFromVersionAndToVersion(
                entityType, entityId, v1, v2);

        if (cached.isPresent()) {
            return deserializeDiff(entityType, entityId, v1, v2, cached.get().getDiffData());
        }

        // Compute diff
        VersionEntity version1 = versionService.getVersionEntity(entityType, entityId, v1);
        VersionEntity version2 = versionService.getVersionEntity(entityType, entityId, v2);

        List<FieldChange> changes = diffEngine.computeDiff(
                version1.getSnapshotData(), version2.getSnapshotData());

        // Cache the result
        persistDiff(entityType, entityId, v1, v2, changes);

        return new VersionDiffResponse(entityType, entityId, v1, v2, changes);
    }

    private void persistDiff(String entityType, UUID entityId, int v1, int v2, List<FieldChange> changes) {
        try {
            VersionDiffEntity diffEntity = new VersionDiffEntity();
            diffEntity.setEntityType(entityType);
            diffEntity.setEntityId(entityId);
            diffEntity.setFromVersion(v1);
            diffEntity.setToVersion(v2);
            diffEntity.setDiffData(objectMapper.writeValueAsString(changes));
            diffRepository.save(diffEntity);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache diff for {}/{} v{}-v{}", entityType, entityId, v1, v2, e);
        }
    }

    private VersionDiffResponse deserializeDiff(String entityType, UUID entityId,
                                                 int v1, int v2, String diffDataJson) {
        try {
            List<FieldChange> changes = objectMapper.readValue(diffDataJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FieldChange.class));
            return new VersionDiffResponse(entityType, entityId, v1, v2, changes);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached diff, recomputing", e);
            VersionEntity version1 = versionService.getVersionEntity(entityType, entityId, v1);
            VersionEntity version2 = versionService.getVersionEntity(entityType, entityId, v2);
            List<FieldChange> changes = diffEngine.computeDiff(
                    version1.getSnapshotData(), version2.getSnapshotData());
            return new VersionDiffResponse(entityType, entityId, v1, v2, changes);
        }
    }
}
