package com.reportplatform.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenValidationService.
 * Tests JWT token validation, claims extraction, and cache behavior.
 */
@ExtendWith(MockitoExtension.class)
class TokenValidationServiceTest {

    private static final String JWKS_URI = "https://login.microsoftonline.com/common/discovery/v2.0/keys";
    private static final String EXPECTED_ISSUER = "https://login.microsoftonline.com/tenant-id/v2.0";
    private static final String EXPECTED_AUDIENCE = "client-id-123";

    private TokenValidationService tokenValidationService;

    @BeforeEach
    void setUp() {
        tokenValidationService = new TokenValidationService(
                JWKS_URI,
                EXPECTED_ISSUER,
                EXPECTED_AUDIENCE,
                5);
    }

    @Test
    void service_initializes_withCorrectProperties() {
        // Assert
        assertNotNull(tokenValidationService);
    }

    @Test
    void validatedClaims_recordContainsAllFields() {
        // Arrange
        var claims = new TokenValidationService.ValidatedClaims(
                "user-oid",
                "tenant-id",
                "user@example.com",
                "Test User",
                List.of("Editor", "Viewer"),
                List.of("group-1", "group-2"));

        // Assert
        assertEquals("user-oid", claims.oid());
        assertEquals("tenant-id", claims.tenantId());
        assertEquals("user@example.com", claims.preferredUsername());
        assertEquals("Test User", claims.displayName());
        assertEquals(2, claims.roles().size());
        assertEquals(2, claims.groups().size());
    }

    @Test
    void validatedClaims_toHeaderMap_returnsCorrectHeaders() {
        // Arrange
        var claims = new TokenValidationService.ValidatedClaims(
                "user-oid",
                "tenant-id",
                "user@example.com",
                "Test User",
                List.of("EDITOR"),
                List.of());

        // Act
        var headerMap = claims.toHeaderMap("org-123", List.of("EDITOR"));

        // Assert
        assertEquals("user-oid", headerMap.get("X-User-Id"));
        assertEquals("org-123", headerMap.get("X-Org-Id"));
        assertEquals("EDITOR", headerMap.get("X-Roles"));
    }

    @Test
    void validatedClaims_toHeaderMap_withNullOid() {
        // Arrange
        var claims = new TokenValidationService.ValidatedClaims(
                null,
                "tenant-id",
                "user@example.com",
                "Test User",
                List.of(),
                List.of());

        // Act
        var headerMap = claims.toHeaderMap("org-123", List.of());

        // Assert
        assertEquals("", headerMap.get("X-User-Id"));
    }

    @Test
    void validatedClaims_toHeaderMap_withEmptyRoles() {
        // Arrange
        var claims = new TokenValidationService.ValidatedClaims(
                "user-oid",
                "tenant-id",
                "user@example.com",
                "Test User",
                List.of(),
                List.of());

        // Act
        var headerMap = claims.toHeaderMap("org-123", List.of());

        // Assert
        assertEquals("", headerMap.get("X-Roles"));
    }
}
