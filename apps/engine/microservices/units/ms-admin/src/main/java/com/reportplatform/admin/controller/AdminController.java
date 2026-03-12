package com.reportplatform.admin.controller;

import com.reportplatform.admin.model.dto.*;
import com.reportplatform.admin.service.ApiKeyService;
import com.reportplatform.admin.service.FailedJobService;
import com.reportplatform.admin.service.OrganizationService;
import com.reportplatform.admin.service.RoleManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    public AdminController(
            OrganizationService organizationService,
            ApiKeyService apiKeyService,
            FailedJobService failedJobService,
            RoleManagementService roleManagementService) {
        this.organizationService = organizationService;
        this.apiKeyService = apiKeyService;
        this.failedJobService = failedJobService;
        this.roleManagementService = roleManagementService;
    }

    // ==================== Organizations ====================

    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationDTO>> listOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/organizations/{orgId}")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getOrganization(orgId));
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationDTO> createOrganization(@RequestBody CreateOrganizationRequest request) {
        OrganizationDTO created = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/organizations/{orgId}")
    public ResponseEntity<OrganizationDTO> updateOrganization(
            @PathVariable UUID orgId,
            @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, request));
    }

    @DeleteMapping("/organizations/{orgId}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID orgId) {
        organizationService.deleteOrganization(orgId);
        return ResponseEntity.noContent().build();
    }

    // ==================== API Keys ====================

    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKeyDTO>> listApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @PostMapping("/api-keys")
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(@RequestBody CreateApiKeyRequest request) {
        // Get user from security context (would come from JWT)
        String createdBy = "system";
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api-keys/{keyId}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID keyId) {
        apiKeyService.revokeApiKey(keyId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Failed Jobs ====================

    @GetMapping("/failed-jobs")
    public ResponseEntity<PaginatedResponse<FailedJobDTO>> listFailedJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String orgId) {
        return ResponseEntity.ok(failedJobService.getFailedJobs(page, pageSize, orgId));
    }

    @PostMapping("/failed-jobs/{jobId}/reprocess")
    public ResponseEntity<Map<String, String>> reprocessFailedJob(@PathVariable UUID jobId) {
        failedJobService.reprocessFailedJob(jobId);
        return ResponseEntity.accepted().body(Map.of(
                "job_id", jobId.toString(),
                "status", "REQUEUED"));
    }

    // ==================== Role Management ====================

    @PostMapping("/roles/assign")
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
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
