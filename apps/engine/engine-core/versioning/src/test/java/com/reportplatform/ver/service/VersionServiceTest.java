package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ver.model.VersionEntity;
import com.reportplatform.ver.model.dto.CreateVersionRequest;
import com.reportplatform.ver.model.dto.VersionResponse;
import com.reportplatform.ver.repository.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VersionService.
 * Tests version creation, locking, and retrieval.
 */
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

    // ==================== createVersion Tests ====================

    @Test
    void createVersion_firstVersion_createsWithVersionOne() {
        // Arrange
        UUID orgId = UUID.randomUUID();

        var request = mock(CreateVersionRequest.class);
        when(request.entityType()).thenReturn("REPORT");
        when(request.entityId()).thenReturn(UUID.randomUUID().toString());
        when(request.snapshotData()).thenReturn(objectMapper.createObjectNode());
        when(request.createdBy()).thenReturn("user-123");
        when(request.reason()).thenReturn("Initial version");

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(anyString(), anyString()))
                .thenReturn(Optional.empty());

        var savedEntity = mock(VersionEntity.class);
        when(savedEntity.getId()).thenReturn(UUID.randomUUID());
        when(savedEntity.getEntityType()).thenReturn("REPORT");
        when(savedEntity.getEntityId()).thenReturn(UUID.randomUUID().toString());
        when(savedEntity.getVersionNumber()).thenReturn(1);
        when(savedEntity.getOrgId()).thenReturn(orgId);

        when(versionRepository.save(any(VersionEntity.class))).thenReturn(savedEntity);

        // Act
        VersionResponse result = versionService.createVersion(request, orgId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.versionNumber());
        verify(daprEventPublisher).publishVersionCreated(any(), eq("REPORT"), any(), eq(1), eq(orgId));
    }

    @Test
    void createVersion_existingVersions_incrementsVersion() {
        // Arrange
        UUID orgId = UUID.randomUUID();

        var previousVersion = mock(VersionEntity.class);
        when(previousVersion.getVersionNumber()).thenReturn(3);

        var request = mock(CreateVersionRequest.class);
        when(request.entityType()).thenReturn("REPORT");
        when(request.entityId()).thenReturn(UUID.randomUUID().toString());
        when(request.snapshotData()).thenReturn(objectMapper.createObjectNode());
        when(request.createdBy()).thenReturn("user-123");
        when(request.reason()).thenReturn("Update");

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(anyString(), anyString()))
                .thenReturn(Optional.of(previousVersion));

        var savedEntity = mock(VersionEntity.class);
        when(savedEntity.getId()).thenReturn(UUID.randomUUID());
        when(savedEntity.getEntityType()).thenReturn("REPORT");
        when(savedEntity.getEntityId()).thenReturn(UUID.randomUUID().toString());
        when(savedEntity.getVersionNumber()).thenReturn(4);
        when(savedEntity.getOrgId()).thenReturn(orgId);

        when(versionRepository.save(any(VersionEntity.class))).thenReturn(savedEntity);

        // Act
        VersionResponse result = versionService.createVersion(request, orgId);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.versionNumber());
    }

    // ==================== listVersions Tests ====================

    @Test
    void listVersions_returnsVersions() {
        // Arrange
        String entityType = "REPORT";
        String entityId = UUID.randomUUID().toString();

        var version = mock(VersionEntity.class);
        when(version.getId()).thenReturn(UUID.randomUUID());
        when(version.getEntityType()).thenReturn(entityType);
        when(version.getEntityId()).thenReturn(entityId);
        when(version.getVersionNumber()).thenReturn(1);

        when(versionRepository.findByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId))
                .thenReturn(List.of(version));

        // Act
        List<VersionResponse> result = versionService.listVersions(entityType, UUID.fromString(entityId));

        // Assert
        assertEquals(1, result.size());
    }

    // ==================== getVersionEntity Tests ====================

    @Test
    void getVersionEntity_exists_returnsVersion() {
        // Arrange
        String entityType = "REPORT";
        UUID entityId = UUID.randomUUID();
        int versionNumber = 1;

        var version = mock(VersionEntity.class);
        when(version.getId()).thenReturn(UUID.randomUUID());

        when(versionRepository.findByEntityTypeAndEntityIdAndVersionNumber(entityType, entityId, versionNumber))
                .thenReturn(Optional.of(version));

        // Act
        VersionEntity result = versionService.getVersionEntity(entityType, entityId, versionNumber);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getVersionEntity_notFound_throwsException() {
        // Arrange
        String entityType = "REPORT";
        UUID entityId = UUID.randomUUID();
        int versionNumber = 999;

        when(versionRepository.findByEntityTypeAndEntityIdAndVersionNumber(entityType, entityId, versionNumber))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> versionService.getVersionEntity(entityType, entityId, versionNumber));
    }

    // ==================== lockVersions Tests ====================

    @Test
    void lockVersions_callsRepository() {
        // Arrange
        String entityType = "REPORT";
        UUID entityId = UUID.randomUUID();

        when(versionRepository.lockAllVersions(entityType, entityId)).thenReturn(3);

        // Act
        versionService.lockVersions(entityType, entityId);

        // Assert
        verify(versionRepository).lockAllVersions(entityType, entityId);
    }

    // ==================== isLatestVersionLocked Tests ====================

    @Test
    void isLatestVersionLocked_whenLocked_returnsTrue() {
        // Arrange
        String entityType = "REPORT";
        UUID entityId = UUID.randomUUID();

        var latestVersion = mock(VersionEntity.class);
        when(latestVersion.getLocked()).thenReturn(true);

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId))
                .thenReturn(Optional.of(latestVersion));

        // Act
        boolean result = versionService.isLatestVersionLocked(entityType, entityId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isLatestVersionLocked_whenUnlocked_returnsFalse() {
        // Arrange
        String entityType = "REPORT";
        UUID entityId = UUID.randomUUID();

        var latestVersion = mock(VersionEntity.class);
        when(latestVersion.getLocked()).thenReturn(false);

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId))
                .thenReturn(Optional.of(latestVersion));

        // Act
        boolean result = versionService.isLatestVersionLocked(entityType, entityId);

        // Assert
        assertFalse(result);
    }

    @Test
    void isLatestVersionLocked_noVersions_returnsFalse() {
        // Arrange
        String entityType = "REPORT";
        UUID entityId = UUID.randomUUID();

        when(versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId))
                .thenReturn(Optional.empty());

        // Act
        boolean result = versionService.isLatestVersionLocked(entityType, entityId);

        // Assert
        assertFalse(result);
    }
}
