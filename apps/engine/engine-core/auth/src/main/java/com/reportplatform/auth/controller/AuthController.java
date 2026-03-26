package com.reportplatform.auth.controller;

import com.reportplatform.auth.model.RoleType;
import com.reportplatform.auth.model.UserRoleEntity;
import com.reportplatform.auth.model.dto.AuthVerifyResponse;
import com.reportplatform.auth.model.dto.SwitchOrgRequest;
import com.reportplatform.auth.model.dto.UserContextResponse;
import com.reportplatform.auth.repository.AuthOrganizationRepository;
import com.reportplatform.auth.repository.UserRoleRepository;
import com.reportplatform.auth.service.ApiKeyService;
import com.reportplatform.auth.service.RbacService;
import com.reportplatform.auth.service.TokenValidationService;
import com.reportplatform.auth.service.TokenValidationService.ValidatedClaims;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Value("${auth.mode:production}")
    private String authMode;

    @Value("${auth.dev-user-oid:6bbc3213-00ac-4d30-bf27-7477b207c515}")
    private String devUserOid;

    private final TokenValidationService tokenValidationService;
    private final RbacService rbacService;
    private final ApiKeyService apiKeyService;
    private final UserRoleRepository userRoleRepository;
    private final AuthOrganizationRepository organizationRepository;

    public AuthController(TokenValidationService tokenValidationService,
                          RbacService rbacService,
                          ApiKeyService apiKeyService,
                          UserRoleRepository userRoleRepository,
                          AuthOrganizationRepository organizationRepository) {
        this.tokenValidationService = tokenValidationService;
        this.rbacService = rbacService;
        this.apiKeyService = apiKeyService;
        this.userRoleRepository = userRoleRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * POST /api/auth/verify - Token validation endpoint.
     * Called by API gateway / sidecar to validate incoming tokens.
     * Returns validated user info in response headers (X-User-Id, X-Org-Id, X-Roles).
     */
    @RequestMapping(value = "/verify", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<AuthVerifyResponse> verifyToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        // Try API key first
        if (apiKey != null && !apiKey.isBlank()) {
            var apiKeyEntity = apiKeyService.validateApiKey(apiKey);
            if (apiKeyEntity.isPresent()) {
                var entity = apiKeyEntity.get();
                String orgId = entity.getOrganization().getId().toString();
                List<String> roles = List.of(entity.getRole().name());

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-User-Id", "apikey:" + entity.getId());
                headers.set("X-Org-Id", orgId);
                headers.set("X-Roles", String.join(",", roles));

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(new AuthVerifyResponse(
                                "apikey:" + entity.getId(),
                                null,
                                orgId,
                                roles,
                                true
                        ));
            }
        }

        // Try Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthVerifyResponse(null, null, null, Collections.emptyList(), false));
        }

        // DEV MODE: skip JWT validation, use dev user identity
        if ("development".equalsIgnoreCase(authMode)) {
            log.info("DEV MODE: bypassing JWT validation for /verify");
            var activeOrgRole = userRoleRepository.findByUserOidAndActiveOrgTrue(devUserOid);
            String orgId = activeOrgRole
                    .map(ur -> ur.getOrganization().getId().toString())
                    .orElse("");

            List<String> devRoles = rbacService.getAllUserRoles(devUserOid).stream()
                    .map(ur -> ur.getRole().name())
                    .toList();
            if (devRoles.isEmpty()) {
                devRoles = List.of("HOLDING_ADMIN");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", devUserOid);
            headers.set("X-Org-Id", orgId);
            headers.set("X-Roles", String.join(",", devRoles));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new AuthVerifyResponse(devUserOid, "dev", orgId, devRoles, true));
        }

        String token = authHeader.substring(7);
        var claimsOpt = tokenValidationService.validateToken(token);

        if (claimsOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthVerifyResponse(null, null, null, Collections.emptyList(), false));
        }

        ValidatedClaims claims = claimsOpt.get();

        // Look up active org and roles for this user
        var activeOrgRole = userRoleRepository.findByUserOidAndActiveOrgTrue(claims.oid());
        String orgId = activeOrgRole
                .map(ur -> ur.getOrganization().getId().toString())
                .orElse("");

        List<String> internalRoles;
        if (activeOrgRole.isPresent()) {
            UUID activeOrgId = activeOrgRole.get().getOrganization().getId();
            internalRoles = rbacService.getUserRolesForOrg(claims.oid(), activeOrgId)
                    .stream()
                    .map(RoleType::name)
                    .toList();
        } else {
            internalRoles = Collections.emptyList();
        }

        Map<String, String> headerMap = claims.toHeaderMap(orgId, internalRoles);
        HttpHeaders headers = new HttpHeaders();
        headerMap.forEach(headers::set);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new AuthVerifyResponse(
                        claims.oid(),
                        claims.tenantId(),
                        orgId,
                        internalRoles,
                        true
                ));
    }

    /**
     * GET /api/auth/me - Returns current user context: organizations, roles, active org.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<UserContextResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userOid = auth.getPrincipal().toString();

        // If the principal looks like an API key reference, return API key context
        if (userOid.startsWith("apikey:")) {
            try {
                String apiKeyIdStr = userOid.substring("apikey:".length());
                UUID apiKeyId = UUID.fromString(apiKeyIdStr);
                var apiKeyOpt = apiKeyService.findById(apiKeyId);
                if (apiKeyOpt.isPresent()) {
                    var apiKey = apiKeyOpt.get();
                    var org = apiKey.getOrganization();
                    String orgId = org != null ? org.getId().toString() : null;
                    String orgCode = org != null ? org.getCode() : null;
                    String orgName = org != null ? org.getName() : null;
                    List<String> roles = List.of(apiKey.getRole().name());
                    var orgRoles = org != null
                            ? List.of(new UserContextResponse.OrgRole(orgId, orgCode, orgName, roles))
                            : List.<UserContextResponse.OrgRole>of();
                    String keyEmail = apiKey.getKeyName() != null
                            ? apiKey.getKeyName().replaceAll("\\s+", ".").toLowerCase() + "@apikey.local"
                            : "apikey@system.local";
                    return ResponseEntity.ok(new UserContextResponse(
                            userOid,
                            apiKey.getKeyName() != null ? apiKey.getKeyName() : "API Key User",
                            keyEmail,
                            orgRoles,
                            orgId
                    ));
                }
            } catch (Exception e) {
                log.warn("Failed to resolve API key context for {}: {}", userOid, e.getMessage());
            }
            return ResponseEntity.ok(new UserContextResponse(
                    userOid, "API Key User", null, List.of(), null));
        }

        try {
            ValidatedClaims claims = null;
            if (auth.getCredentials() instanceof ValidatedClaims vc) {
                claims = vc;
            }

            List<UserRoleEntity> allRoles = rbacService.getAllUserRoles(userOid);

            // Group by organization
            Map<UUID, List<UserRoleEntity>> byOrg = allRoles.stream()
                    .collect(Collectors.groupingBy(ur -> ur.getOrganization().getId()));

            List<UserContextResponse.OrgRole> orgRoles = byOrg.entrySet().stream()
                    .map(entry -> {
                        var firstRole = entry.getValue().getFirst();
                        var org = firstRole.getOrganization();
                        List<String> roles = entry.getValue().stream()
                                .map(ur -> ur.getRole().name())
                                .toList();
                        return new UserContextResponse.OrgRole(
                                org.getId().toString(),
                                org.getCode(),
                                org.getName(),
                                roles
                        );
                    })
                    .toList();

            String activeOrgId = allRoles.stream()
                    .filter(UserRoleEntity::isActiveOrg)
                    .findFirst()
                    .map(ur -> ur.getOrganization().getId().toString())
                    .orElse(null);

            return ResponseEntity.ok(new UserContextResponse(
                    userOid,
                    claims != null ? claims.displayName() : null,
                    claims != null ? claims.preferredUsername() : null,
                    orgRoles,
                    activeOrgId
            ));
        } catch (Exception e) {
            log.warn("Failed to get user context for {}: {}", userOid, e.getMessage());
            // Fallback: try to look up the credentials as an API key
            try {
                Object creds = auth.getCredentials();
                if (creds instanceof String rawToken) {
                    var apiKeyOpt = apiKeyService.validateApiKey(rawToken);
                    if (apiKeyOpt.isPresent()) {
                        var apiKey = apiKeyOpt.get();
                        String orgId = apiKey.getOrganization().getId().toString();
                        String orgName = apiKey.getOrganization().getName();
                        String orgCode = apiKey.getOrganization().getCode();
                        List<String> roles = List.of(apiKey.getRole().name());
                        return ResponseEntity.ok(new UserContextResponse(
                                "apikey:" + apiKey.getId(),
                                apiKey.getKeyName(),
                                null,
                                List.of(new UserContextResponse.OrgRole(orgId, orgCode, orgName, roles)),
                                orgId
                        ));
                    }
                }
            } catch (Exception ex) {
                log.warn("API key fallback also failed: {}", ex.getMessage());
            }
            // Return minimal context to avoid 500
            return ResponseEntity.ok(new UserContextResponse(
                    userOid, null, null, List.of(), null));
        }
    }

    /**
     * POST /api/auth/refresh - Refresh/revalidate token.
     * In production, token refresh is handled by MSAL on the client side.
     * This endpoint revalidates the current token and returns updated context.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthVerifyResponse> refreshToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {

        // Extract token from body or header
        String token = null;
        if (body != null && body.containsKey("token")) {
            token = body.get("token");
        }
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthVerifyResponse(null, null, null, Collections.emptyList(), false));
        }

        // In dev mode, just return success with current user info
        if ("development".equalsIgnoreCase(authMode)) {
            var activeOrgRole = userRoleRepository.findByUserOidAndActiveOrgTrue(devUserOid);
            String orgId = activeOrgRole
                    .map(ur -> ur.getOrganization().getId().toString())
                    .orElse("");
            List<String> devRoles = rbacService.getAllUserRoles(devUserOid).stream()
                    .map(ur -> ur.getRole().name())
                    .toList();
            if (devRoles.isEmpty()) devRoles = List.of("HOLDING_ADMIN");

            return ResponseEntity.ok(new AuthVerifyResponse(devUserOid, "dev", orgId, devRoles, true));
        }

        // Validate and return refreshed context
        return verifyToken("Bearer " + token, null);
    }

    /**
     * POST /api/auth/switch-org - Validates user membership and updates active organization.
     */
    @PostMapping("/switch-org")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<UserContextResponse> switchOrganization(
            @Valid @RequestBody SwitchOrgRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userOid = auth.getPrincipal().toString();
        UUID targetOrgId;
        try {
            targetOrgId = UUID.fromString(request.organizationId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        // Verify the organization exists
        var orgOpt = organizationRepository.findById(targetOrgId);
        if (orgOpt.isEmpty() || !orgOpt.get().isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Verify user has membership in this org
        List<UserRoleEntity> userRolesInOrg =
                userRoleRepository.findByUserOidAndOrganizationId(userOid, targetOrgId);
        if (userRolesInOrg.isEmpty()) {
            log.warn("User {} attempted to switch to org {} without membership", userOid, targetOrgId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Clear current active org and set new one
        userRoleRepository.clearActiveOrg(userOid);
        userRoleRepository.setActiveOrg(userOid, targetOrgId);

        // Return updated user context
        return getCurrentUser();
    }
}
