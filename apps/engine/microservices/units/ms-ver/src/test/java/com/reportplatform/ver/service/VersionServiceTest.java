package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reportplatform.ver.model.VersionEntity;
import com.reportplatform.ver.model.dto.CreateVersionRequest;
import com.reportplatform.ver.model.dto.VersionResponse;
import com.reportplatform.ver.repository.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private VersionRepository versionRepository;

    @Mock
    private DaprEventPublisher daprEventPublisher;

    private VersionService versionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        versionService = new VersionService(versionRepository, daprEventPublisher, objectMapper);
    }

    @Test
    void createVersion_shouldIncrementVersionNumber() {
        UUID entityId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        ObjectNode snapshot = objectMapper.createObjectNode().put("name", "Test");

        VersionEntity existing = new VersionEntity();
        existing.setVersionNumber(2);

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(
                "TABLE_RECORD", entityId)).thenReturn(Optional.of(existing));

        VersionEntity saved = new VersionEntity();
        saved.setId(UUID.randomUUID());
        saved.setEntityType("TABLE_RECORD");
        saved.setEntityId(entityId);
        saved.setVersionNumber(3);
        saved.setOrgId(orgId);
        saved.setCreatedBy("user1");
        saved.setCreatedAt(Instant.now());
        saved.setSnapshotData(snapshot.toString());

        when(versionRepository.save(any())).thenReturn(saved);

        CreateVersionRequest request = new CreateVersionRequest(
                "TABLE_RECORD", entityId, snapshot, "test", "user1");

        VersionResponse response = versionService.createVersion(request, orgId);

        assertThat(response.versionNumber()).isEqualTo(3);
        verify(daprEventPublisher).publishVersionCreated(any(), eq("TABLE_RECORD"), eq(entityId), eq(3), eq(orgId));
    }

    @Test
    void createVersion_shouldStartAtV1ForNewEntity() {
        UUID entityId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        ObjectNode snapshot = objectMapper.createObjectNode().put("name", "New");

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(
                "TABLE_RECORD", entityId)).thenReturn(Optional.empty());

        VersionEntity saved = new VersionEntity();
        saved.setId(UUID.randomUUID());
        saved.setEntityType("TABLE_RECORD");
        saved.setEntityId(entityId);
        saved.setVersionNumber(1);
        saved.setOrgId(orgId);
        saved.setCreatedBy("user1");
        saved.setCreatedAt(Instant.now());
        saved.setSnapshotData(snapshot.toString());

        when(versionRepository.save(any())).thenReturn(saved);

        CreateVersionRequest request = new CreateVersionRequest(
                "TABLE_RECORD", entityId, snapshot, null, "user1");

        VersionResponse response = versionService.createVersion(request, orgId);

        assertThat(response.versionNumber()).isEqualTo(1);
    }

    @Test
    void lockVersions_shouldCallRepository() {
        UUID entityId = UUID.randomUUID();

        when(versionRepository.lockAllVersions("TABLE_RECORD", entityId)).thenReturn(3);

        versionService.lockVersions("TABLE_RECORD", entityId);

        verify(versionRepository).lockAllVersions("TABLE_RECORD", entityId);
    }

    @Test
    void createVersionOnLockedEntity_shouldPublishEditOnLockedEvent() {
        UUID entityId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        ObjectNode snapshot = objectMapper.createObjectNode().put("name", "Edit");

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(
                "TABLE_RECORD", entityId)).thenReturn(Optional.empty());

        VersionEntity saved = new VersionEntity();
        saved.setId(UUID.randomUUID());
        saved.setEntityType("TABLE_RECORD");
        saved.setEntityId(entityId);
        saved.setVersionNumber(1);
        saved.setOrgId(orgId);
        saved.setCreatedBy("user1");
        saved.setCreatedAt(Instant.now());
        saved.setSnapshotData(snapshot.toString());

        when(versionRepository.save(any())).thenReturn(saved);

        CreateVersionRequest request = new CreateVersionRequest(
                "TABLE_RECORD", entityId, snapshot, "edit on locked", "user1");

        versionService.createVersionOnLockedEntity(request, orgId);

        verify(daprEventPublisher).publishEditOnLocked("TABLE_RECORD", entityId, 1, orgId);
    }
}
