package com.reportplatform.auth.controller;

import com.reportplatform.auth.model.dto.AuthVerifyResponse;
import com.reportplatform.auth.model.dto.UserContextResponse;
import com.reportplatform.auth.repository.AuthOrganizationRepository;
import com.reportplatform.auth.repository.UserRoleRepository;
import com.reportplatform.auth.service.ApiKeyService;
import com.reportplatform.auth.service.RbacService;
import com.reportplatform.auth.service.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 * Tests authentication endpoints: verifyToken, getCurrentUser,
 * switchOrganization
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private TokenValidationService tokenValidationService;

    @Mock
    private RbacService rbacService;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuthOrganizationRepository organizationRepository;

    @Mock
    private Authentication authentication;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                tokenValidationService,
                rbacService,
                apiKeyService,
                userRoleRepository,
                organizationRepository);
    }

    // ==================== verifyToken Tests ====================

    @Test
    void verifyToken_withValidToken_returnsUserInfo() {
        // Arrange
        String validToken = "valid.jwt.token";
        TokenValidationService.ValidatedClaims claims = new TokenValidationService.ValidatedClaims(
                "user-oid-123",
                "tenant-456",
                "user@example.com",
                "Test User",
                List.of("EDITOR"),
                Collections.emptyList());

        when(tokenValidationService.validateToken(validToken)).thenReturn(Optional.of(claims));
        when(userRoleRepository.findByUserOidAndActiveOrgTrue("user-oid-123")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<AuthVerifyResponse> response = authController.verifyToken(
                "Bearer " + validToken, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isValid());
        assertEquals("user-oid-123", response.getBody().userId());
    }

    @Test
    void verifyToken_withNoAuthHeader_returnsUnauthorized() {
        // Act
        ResponseEntity<AuthVerifyResponse> response = authController.verifyToken(null, null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isValid());
    }

    @Test
    void verifyToken_withInvalidToken_returnsUnauthorized() {
        // Arrange
        when(tokenValidationService.validateToken(anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<AuthVerifyResponse> response = authController.verifyToken(
                "Bearer invalid.token", null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isValid());
    }

    @Test
    void verifyToken_withValidApiKey_returnsUserInfo() {
        // Arrange
        String apiKey = "test-api-key";
        var apiKeyEntity = mock(com.reportplatform.auth.model.entity.ApiKeyEntity.class);
        var org = mock(com.reportplatform.auth.model.entity.OrganizationEntity.class);

        when(apiKeyEntity.getId()).thenReturn(UUID.randomUUID());
        when(apiKeyEntity.getRole()).thenReturn(com.reportplatform.auth.model.RoleType.EDITOR);
        when(apiKeyEntity.getOrganization()).thenReturn(org);
        when(org.getId()).thenReturn(UUID.randomUUID());

        when(apiKeyService.validateApiKey(apiKey)).thenReturn(Optional.of(apiKeyEntity));

        // Act
        ResponseEntity<AuthVerifyResponse> response = authController.verifyToken(null, apiKey);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isValid());
        assertTrue(response.getBody().userId().startsWith("apikey:"));
    }

    // ==================== getCurrentUser Tests ====================

    @Test
    void getCurrentUser_withAuthenticatedUser_returnsUserContext() {
        // Arrange
        setupSecurityContext();

        var userRoleEntity = mock(com.reportplatform.auth.model.UserRoleEntity.class);
        var org = mock(com.reportplatform.auth.model.entity.OrganizationEntity.class);

        when(org.getId()).thenReturn(UUID.randomUUID());
        when(org.getCode()).thenReturn("ORG-001");
        when(org.getName()).thenReturn("Test Organization");
        when(userRoleEntity.getOrganization()).thenReturn(org);
        when(userRoleEntity.getRole()).thenReturn(com.reportplatform.auth.model.RoleType.EDITOR);
        when(userRoleEntity.isActiveOrg()).thenReturn(true);

        when(rbacService.getAllUserRoles(anyString())).thenReturn(List.of(userRoleEntity));

        // Act
        ResponseEntity<UserContextResponse> response = authController.getCurrentUser();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getCurrentUser_withNoAuthentication_returnsUnauthorized() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act
        ResponseEntity<UserContextResponse> response = authController.getCurrentUser();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ==================== switchOrganization Tests ====================

    @Test
    void switchOrganization_withValidOrg_updatesActiveOrg() {
        // Arrange
        String userOid = "user-oid-123";
        UUID targetOrgId = UUID.randomUUID();

        setupSecurityContext();

        var org = mock(com.reportplatform.auth.model.entity.OrganizationEntity.class);
        when(org.getId()).thenReturn(targetOrgId);
        when(org.isActive()).thenReturn(true);

        var userRoleEntity = mock(com.reportplatform.auth.model.UserRoleEntity.class);

        when(organizationRepository.findById(targetOrgId)).thenReturn(Optional.of(org));
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, targetOrgId))
                .thenReturn(List.of(userRoleEntity));

        var request = new com.reportplatform.auth.model.dto.SwitchOrgRequest(targetOrgId.toString());

        // Act
        ResponseEntity<UserContextResponse> response = authController.switchOrganization(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRoleRepository).clearActiveOrg(userOid);
        verify(userRoleRepository).setActiveOrg(userOid, targetOrgId);
    }

    @Test
    void switchOrganization_withInvalidOrgId_returnsBadRequest() {
        // Arrange
        setupSecurityContext();

        var request = new com.reportplatform.auth.model.dto.SwitchOrgRequest("invalid-uuid");

        // Act
        ResponseEntity<UserContextResponse> response = authController.switchOrganization(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void switchOrganization_withNonExistentOrg_returnsNotFound() {
        // Arrange
        setupSecurityContext();
        UUID targetOrgId = UUID.randomUUID();

        when(organizationRepository.findById(targetOrgId)).thenReturn(Optional.empty());

        var request = new com.reportplatform.auth.model.dto.SwitchOrgRequest(targetOrgId.toString());

        // Act
        ResponseEntity<UserContextResponse> response = authController.switchOrganization(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void switchOrganization_withoutMembership_returnsForbidden() {
        // Arrange
        String userOid = "user-oid-123";
        UUID targetOrgId = UUID.randomUUID();

        setupSecurityContext();

        var org = mock(com.reportplatform.auth.model.entity.OrganizationEntity.class);
        when(org.getId()).thenReturn(targetOrgId);
        when(org.isActive()).thenReturn(true);

        when(organizationRepository.findById(targetOrgId)).thenReturn(Optional.of(org));
        when(userRoleRepository.findByUserOidAndOrganizationId(userOid, targetOrgId))
                .thenReturn(Collections.emptyList());

        var request = new com.reportplatform.auth.model.dto.SwitchOrgRequest(targetOrgId.toString());

        // Act
        ResponseEntity<UserContextResponse> response = authController.switchOrganization(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Helper method to set up security context
    private void setupSecurityContext() {
        var claims = new TokenValidationService.ValidatedClaims(
                "user-oid-123", "tenant-456", "user@example.com",
                "Test User", List.of("EDITOR"), Collections.emptyList());

        when(authentication.getPrincipal()).thenReturn("user-oid-123");
        when(authentication.getCredentials()).thenReturn(claims);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
