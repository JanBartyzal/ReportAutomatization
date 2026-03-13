package com.reportplatform.auth.service;

import com.reportplatform.auth.model.RoleType;
import com.reportplatform.auth.model.UserRoleEntity;
import com.reportplatform.auth.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RbacService.
 * Tests role permission checks and AAD group mapping.
 */
@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new RbacService(userRoleRepository);
    }

    // ==================== getUserRolesForOrg Tests ====================

    @Test
    void getUserRolesForOrg_returnsRolesForOrganization() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();

        var userRole = mock(UserRoleEntity.class);
        when(userRole.getRole()).thenReturn(RoleType.EDITOR);
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of(userRole));

        // Act
        List<RoleType> roles = rbacService.getUserRolesForOrg(userOid, orgId);

        // Assert
        assertEquals(1, roles.size());
        assertEquals(RoleType.EDITOR, roles.get(0));
    }

    @Test
    void getUserRolesForOrg_withNoRoles_returnsEmptyList() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of());

        // Act
        List<RoleType> roles = rbacService.getUserRolesForOrg(userOid, orgId);

        // Assert
        assertTrue(roles.isEmpty());
    }

    // ==================== hasPermission Tests ====================

    @Test
    void hasPermission_withRequiredRole_returnsTrue() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();

        var userRole = mock(UserRoleEntity.class);
        when(userRole.getRole()).thenReturn(RoleType.ADMIN);
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of(userRole));

        // Act
        boolean result = rbacService.hasPermission(userOid, orgId, RoleType.EDITOR);

        // Assert
        assertTrue(result);
    }

    @Test
    void hasPermission_withoutRequiredRole_returnsFalse() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();

        var userRole = mock(UserRoleEntity.class);
        when(userRole.getRole()).thenReturn(RoleType.VIEWER);
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of(userRole));

        // Act
        boolean result = rbacService.hasPermission(userOid, orgId, RoleType.EDITOR);

        // Assert
        assertFalse(result);
    }

    @Test
    void hasPermission_withNoRoles_returnsFalse() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of());

        // Act
        boolean result = rbacService.hasPermission(userOid, orgId, RoleType.VIEWER);

        // Assert
        assertFalse(result);
    }

    // ==================== getHighestRole Tests ====================

    @Test
    void getHighestRole_returnsHighestPrivilegeRole() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();

        var viewerRole = mock(UserRoleEntity.class);
        when(viewerRole.getRole()).thenReturn(RoleType.VIEWER);

        var editorRole = mock(UserRoleEntity.class);
        when(editorRole.getRole()).thenReturn(RoleType.EDITOR);

        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of(viewerRole, editorRole));

        // Act
        RoleType highestRole = rbacService.getHighestRole(userOid, orgId);

        // Assert
        assertNotNull(highestRole);
        assertEquals(RoleType.EDITOR, highestRole);
    }

    @Test
    void getHighestRole_withNoRoles_returnsNull() {
        // Arrange
        String userOid = "user-123";
        UUID orgId = UUID.randomUUID();
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, orgId))
                .thenReturn(List.of());

        // Act
        RoleType highestRole = rbacService.getHighestRole(userOid, orgId);

        // Assert
        assertNull(highestRole);
    }

    // ==================== mapAadGroupsToRoles Tests ====================

    @Test
    void mapAadGroupsToRoles_withValidGroups_returnsRoleMapping() {
        // Arrange
        List<String> groups = List.of(
                "ReportPlatform-ORG001-admin",
                "ReportPlatform-ORG001-editor",
                "ReportPlatform-ORG002-viewer",
                "OtherGroup-admin");

        // Act
        var result = rbacService.mapAadGroupsToRoles(groups);

        // Assert
        assertTrue(result.containsKey("ORG001"));
        assertTrue(result.get("ORG001").contains(RoleType.ADMIN));
        assertTrue(result.get("ORG001").contains(RoleType.EDITOR));
        assertTrue(result.containsKey("ORG002"));
        assertTrue(result.get("ORG002").contains(RoleType.VIEWER));
    }

    @Test
    void mapAadGroupsToRoles_withEmptyList_returnsEmptyMap() {
        // Act
        var result = rbacService.mapAadGroupsToRoles(List.of());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void mapAadGroupsToRoles_withNull_returnsEmptyMap() {
        // Act
        var result = rbacService.mapAadGroupsToRoles(null);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== getAllUserRoles Tests ====================

    @Test
    void getAllUserRoles_returnsAllRoles() {
        // Arrange
        String userOid = "user-123";

        var userRole1 = mock(UserRoleEntity.class);
        var userRole2 = mock(UserRoleEntity.class);

        when(userRoleRepository.findByUserOid(userOid))
                .thenReturn(List.of(userRole1, userRole2));

        // Act
        var roles = rbacService.getAllUserRoles(userOid);

        // Assert
        assertEquals(2, roles.size());
    }
}
