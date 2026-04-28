package com.reportplatform.admin.controller;

import com.reportplatform.admin.model.dto.*;
import com.reportplatform.admin.service.ApiKeyService;
import com.reportplatform.admin.service.FailedJobService;
import com.reportplatform.admin.service.HealthDashboardService;
import com.reportplatform.admin.service.HealthServiceRegistryService;
import com.reportplatform.admin.service.OrganizationService;
import com.reportplatform.admin.service.RoleManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final OrganizationService organizationService;
    private final ApiKeyService apiKeyService;
    private final FailedJobService failedJobService;
    private final RoleManagementService roleManagementService;
    private final HealthDashboardService healthDashboardService;
    private final HealthServiceRegistryService healthServiceRegistryService;

    public AdminController(
            OrganizationService organizationService,
            ApiKeyService apiKeyService,
            FailedJobService failedJobService,
            RoleManagementService roleManagementService,
            HealthDashboardService healthDashboardService,
            HealthServiceRegistryService healthServiceRegistryService) {
        this.organizationService = organizationService;
        this.apiKeyService = apiKeyService;
        this.failedJobService = failedJobService;
        this.roleManagementService = roleManagementService;
        this.healthDashboardService = healthDashboardService;
        this.healthServiceRegistryService = healthServiceRegistryService;
    }

    // ==================== Organizations ====================

    @GetMapping("/organizations")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<OrganizationDTO>> listOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/organizations/{orgId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getOrganization(orgId));
    }

    @PostMapping("/organizations")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<OrganizationDTO> createOrganization(@RequestBody CreateOrganizationRequest request) {
        OrganizationDTO created = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/organizations/{orgId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<OrganizationDTO> updateOrganization(
            @PathVariable UUID orgId,
            @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, request));
    }

    @DeleteMapping("/organizations/{orgId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID orgId) {
        organizationService.deleteOrganization(orgId);
        return ResponseEntity.noContent().build();
    }

    // ==================== API Keys ====================

    @GetMapping("/api-keys")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<ApiKeyDTO>> listApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @PostMapping("/api-keys")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(
            @RequestBody CreateApiKeyRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        // Set orgId from header if not provided in request body
        if (request.getOrgId() == null && orgId != null && !orgId.isBlank()) {
            try {
                request.setOrgId(UUID.fromString(orgId));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid X-Org-Id header value '{}': {}", orgId, e.getMessage());
                return ResponseEntity.badRequest().build();
            }
        }
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api-keys/{keyId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID keyId) {
        apiKeyService.revokeApiKey(keyId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Failed Jobs ====================

    @GetMapping("/failed-jobs")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PaginatedResponse<FailedJobDTO>> listFailedJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String orgId) {
        return ResponseEntity.ok(failedJobService.getFailedJobs(page, pageSize, orgId));
    }

    @PostMapping("/failed-jobs/{jobId}/reprocess")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> reprocessFailedJob(@PathVariable UUID jobId) {
        failedJobService.reprocessFailedJob(jobId);
        return ResponseEntity.accepted().body(Map.of(
                "job_id", jobId.toString(),
                "status", "REQUEUED"));
    }

    // ==================== Users ====================

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PaginatedResponse<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(name = "org_id", required = false) UUID orgId) {
        return ResponseEntity.ok(roleManagementService.listUsers(page, pageSize, orgId));
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<RoleAssignmentResponse> assignUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String callerId,
            @RequestHeader(value = "X-Roles", defaultValue = "VIEWER") String roles,
            HttpServletRequest httpRequest) {
        var request = new RoleAssignmentRequest(userId, UUID.fromString(body.get("org_id")), body.get("role"));
        String callerRole = extractHighestRole(roles);
        var response = roleManagementService.assignRole(
                request, callerId, callerRole, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/users/{userId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> removeUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String callerId,
            @RequestHeader(value = "X-Roles", defaultValue = "VIEWER") String roles,
            HttpServletRequest httpRequest) {
        var request = new RoleAssignmentRequest(userId, UUID.fromString(body.get("org_id")), body.get("role"));
        String callerRole = extractHighestRole(roles);
        roleManagementService.revokeRole(
                request, callerId, callerRole, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    // ==================== Role Management ====================

    @PostMapping("/roles/assign")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<RoleAssignmentResponse> assignRole(
            @Valid @RequestBody RoleAssignmentRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Roles") String roles,
            HttpServletRequest httpRequest) {
        String callerRole = extractHighestRole(roles);
        var response = roleManagementService.assignRole(
                request, userId, callerRole, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/roles/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> revokeRole(
            @Valid @RequestBody RoleAssignmentRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Roles") String roles,
            HttpServletRequest httpRequest) {
        String callerRole = extractHighestRole(roles);
        roleManagementService.revokeRole(
                request, userId, callerRole, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/org/{orgId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listRolesForOrg(@PathVariable UUID orgId) {
        return ResponseEntity.ok(roleManagementService.listRolesForOrg(orgId));
    }

    private String extractHighestRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return "VIEWER";
        String[] parts = rolesHeader.split(",");
        // Priority: HOLDING_ADMIN > ADMIN > COMPANY_ADMIN > EDITOR > VIEWER
        for (String priority : List.of("HOLDING_ADMIN", "ADMIN", "COMPANY_ADMIN", "EDITOR", "VIEWER")) {
            for (String part : parts) {
                if (part.trim().equalsIgnoreCase(priority)) return priority;
            }
        }
        return "VIEWER";
    }

    // ==================== Health ====================

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/health/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<HealthDashboardDTO> getHealthDashboard() {
        return ResponseEntity.ok(healthDashboardService.getDashboard());
    }

    // ==================== Health Service Registry ====================

    @GetMapping("/health/services")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<HealthServiceRegistryDTO>> listHealthServices() {
        return ResponseEntity.ok(healthServiceRegistryService.listAll());
    }

    @PostMapping("/health/services")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<HealthServiceRegistryDTO> createHealthService(
            @Valid @RequestBody CreateHealthServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthServiceRegistryService.create(request));
    }

    @PutMapping("/health/services/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<HealthServiceRegistryDTO> updateHealthService(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHealthServiceRequest request) {
        return ResponseEntity.ok(healthServiceRegistryService.update(id, request));
    }

    @DeleteMapping("/health/services/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteHealthService(@PathVariable UUID id) {
        healthServiceRegistryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
