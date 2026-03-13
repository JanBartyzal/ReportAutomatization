package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.CreateOrganizationRequest;
import com.reportplatform.admin.model.dto.OrganizationDTO;
import com.reportplatform.admin.model.entity.OrganizationEntity;
import com.reportplatform.admin.repository.OrganizationRepository;
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
 * Unit tests for OrganizationService.
 * Tests organization CRUD operations and hierarchy management.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(organizationRepository);
    }

    // ==================== getAllOrganizations Tests ====================

    @Test
    void getAllOrganizations_returnsRootOrganizations() {
        // Arrange
        var org = mock(OrganizationEntity.class);
        when(org.getId()).thenReturn(UUID.randomUUID());
        when(org.getCode()).thenReturn("ORG001");
        when(org.getName()).thenReturn("Test Org");
        when(org.getType()).thenReturn(OrganizationEntity.OrganizationType.HOLDING);

        when(organizationRepository.findTopLevelOrganizations()).thenReturn(List.of(org));
        when(organizationRepository.findByParentId(any())).thenReturn(List.of());

        // Act
        List<OrganizationDTO> result = organizationService.getAllOrganizations();

        // Assert
        assertEquals(1, result.size());
        assertEquals("ORG001", result.get(0).getCode());
    }

    @Test
    void getAllOrganizations_withChildren_includesChildOrganizations() {
        // Arrange
        var parent = mock(OrganizationEntity.class);
        when(parent.getId()).thenReturn(UUID.randomUUID());
        when(parent.getCode()).thenReturn("PARENT");
        when(parent.getName()).thenReturn("Parent Org");
        when(parent.getType()).thenReturn(OrganizationEntity.OrganizationType.HOLDING);

        var child = mock(OrganizationEntity.class);
        when(child.getId()).thenReturn(UUID.randomUUID());
        when(child.getCode()).thenReturn("CHILD");
        when(child.getName()).thenReturn("Child Org");
        when(child.getType()).thenReturn(OrganizationEntity.OrganizationType.COMPANY);

        when(organizationRepository.findTopLevelOrganizations()).thenReturn(List.of(parent));
        when(organizationRepository.findByParentId(parent.getId())).thenReturn(List.of(child));

        // Act
        List<OrganizationDTO> result = organizationService.getAllOrganizations();

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("CHILD", result.get(0).getChildren().get(0).getCode());
    }

    // ==================== getOrganization Tests ====================

    @Test
    void getOrganization_exists_returnsOrganization() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var org = mock(OrganizationEntity.class);
        when(org.getId()).thenReturn(orgId);
        when(org.getCode()).thenReturn("ORG001");
        when(org.getName()).thenReturn("Test Org");
        when(org.getType()).thenReturn(OrganizationEntity.OrganizationType.COMPANY);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        // Act
        OrganizationDTO result = organizationService.getOrganization(orgId);

        // Assert
        assertNotNull(result);
        assertEquals("ORG001", result.getCode());
    }

    @Test
    void getOrganization_notFound_throwsException() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> organizationService.getOrganization(orgId));
    }

    // ==================== createOrganization Tests ====================

    @Test
    void createOrganization_validRequest_createsOrganization() {
        // Arrange
        var request = new CreateOrganizationRequest();
        request.setName("New Organization");
        request.setType("COMPANY");

        var savedOrg = mock(OrganizationEntity.class);
        when(savedOrg.getId()).thenReturn(UUID.randomUUID());
        when(savedOrg.getCode()).thenReturn("NEWORG");
        when(savedOrg.getName()).thenReturn("New Organization");
        when(savedOrg.getType()).thenReturn(OrganizationEntity.OrganizationType.COMPANY);

        when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedOrg);

        // Act
        OrganizationDTO result = organizationService.createOrganization(request);

        // Assert
        assertNotNull(result);
        assertEquals("NEWORG", result.getCode());
        verify(organizationRepository).save(any(OrganizationEntity.class));
    }

    @Test
    void createOrganization_withParent_verifiesParentExists() {
        // Arrange
        UUID parentId = UUID.randomUUID();

        var request = new CreateOrganizationRequest();
        request.setName("Child Organization");
        request.setType("DIVISION");
        request.setParentId(parentId);

        var parent = mock(OrganizationEntity.class);
        when(parent.getId()).thenReturn(parentId);

        when(organizationRepository.findById(parentId)).thenReturn(Optional.of(parent));

        var savedOrg = mock(OrganizationEntity.class);
        when(savedOrg.getId()).thenReturn(UUID.randomUUID());
        when(savedOrg.getCode()).thenReturn("CHILD");
        when(savedOrg.getName()).thenReturn("Child Organization");
        when(savedOrg.getType()).thenReturn(OrganizationEntity.OrganizationType.DIVISION);
        when(savedOrg.getParent()).thenReturn(parent);

        when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedOrg);

        // Act
        OrganizationDTO result = organizationService.createOrganization(request);

        // Assert
        assertNotNull(result);
        verify(organizationRepository).findById(parentId);
    }

    @Test
    void createOrganization_invalidParent_throwsException() {
        // Arrange
        UUID parentId = UUID.randomUUID();

        var request = new CreateOrganizationRequest();
        request.setName("Child Organization");
        request.setType("DIVISION");
        request.setParentId(parentId);

        when(organizationRepository.findById(parentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> organizationService.createOrganization(request));
    }

    // ==================== updateOrganization Tests ====================

    @Test
    void updateOrganization_exists_updatesOrganization() {
        // Arrange
        UUID orgId = UUID.randomUUID();

        var request = new CreateOrganizationRequest();
        request.setName("Updated Name");
        request.setType("COMPANY");

        var existingOrg = mock(OrganizationEntity.class);
        when(existingOrg.getId()).thenReturn(orgId);

        var updatedOrg = mock(OrganizationEntity.class);
        when(updatedOrg.getId()).thenReturn(orgId);
        when(updatedOrg.getCode()).thenReturn("ORG001");
        when(updatedOrg.getName()).thenReturn("Updated Name");
        when(updatedOrg.getType()).thenReturn(OrganizationEntity.OrganizationType.COMPANY);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(updatedOrg);

        // Act
        OrganizationDTO result = organizationService.updateOrganization(orgId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
    }

    @Test
    void updateOrganization_notFound_throwsException() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var request = new CreateOrganizationRequest();
        request.setName("Updated Name");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> organizationService.updateOrganization(orgId, request));
    }

    // ==================== deleteOrganization Tests ====================

    @Test
    void deleteOrganization_noChildren_deletesOrganization() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var org = mock(OrganizationEntity.class);
        when(org.getId()).thenReturn(orgId);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(organizationRepository.hasChildren(orgId)).thenReturn(false);

        // Act
        organizationService.deleteOrganization(orgId);

        // Assert
        verify(organizationRepository).delete(org);
    }

    @Test
    void deleteOrganization_withChildren_throwsException() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var org = mock(OrganizationEntity.class);
        when(org.getId()).thenReturn(orgId);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(organizationRepository.hasChildren(orgId)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> organizationService.deleteOrganization(orgId));
    }
}
