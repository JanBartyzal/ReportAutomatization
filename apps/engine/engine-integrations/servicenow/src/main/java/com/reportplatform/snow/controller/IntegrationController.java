package com.reportplatform.snow.controller;

import com.reportplatform.snow.model.dto.ConnectionDTO;
import com.reportplatform.snow.model.dto.CreateConnectionRequest;
import com.reportplatform.snow.model.dto.CreateResolverGroupRequest;
import com.reportplatform.snow.model.dto.ItsmSummaryDto;
import com.reportplatform.snow.model.dto.ProjectSyncConfigDto;
import com.reportplatform.snow.model.dto.ResolverGroupDto;
import com.reportplatform.snow.model.dto.TestConnectionRequest;
import com.reportplatform.snow.model.dto.TestConnectionResponse;
import com.reportplatform.snow.model.dto.UpsertProjectSyncConfigRequest;
import com.reportplatform.snow.service.ConnectionConfigService;
import com.reportplatform.snow.service.DataFetchService;
import com.reportplatform.snow.service.ItsmSyncService;
import com.reportplatform.snow.service.ProjectFetchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/integrations/servicenow")
public class IntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationController.class);

    private final ConnectionConfigService connectionConfigService;
    private final DataFetchService dataFetchService;
    private final ItsmSyncService itsmSyncService;
    private final ProjectFetchService projectFetchService;

    public IntegrationController(ConnectionConfigService connectionConfigService,
                                 DataFetchService dataFetchService,
                                 ItsmSyncService itsmSyncService,
                                 ProjectFetchService projectFetchService) {
        this.connectionConfigService = connectionConfigService;
        this.dataFetchService = dataFetchService;
        this.itsmSyncService = itsmSyncService;
        this.projectFetchService = projectFetchService;
    }

    // ==================== Connections ====================

    @GetMapping
    public ResponseEntity<?> listConnections(
            @RequestHeader(value = "X-Org-Id") UUID orgId) {
        try {
            return ResponseEntity.ok(connectionConfigService.getAllConnections(orgId));
        } catch (Exception e) {
            logger.error("Failed to list connections for org {}: {}", orgId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list connections", "detail", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConnection(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(connectionConfigService.getConnection(id));
        } catch (Exception e) {
            logger.warn("Connection not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Connection not found", "id", id.toString()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createConnection(
            @RequestHeader(value = "X-Org-Id") UUID orgId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @RequestBody Map<String, Object> rawRequest) {
        try {
            // Map both snake_case and camelCase field names
            String instanceUrl = getStringField(rawRequest, "instanceUrl", "instance_url", null);
            if (instanceUrl == null || instanceUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "instanceUrl (or instance_url) is required"));
            }
            String credentialsRef = getStringField(rawRequest, "credentialsRef", "credentials_ref", null);
            if (credentialsRef == null || credentialsRef.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "credentialsRef (or credentials_ref) is required"));
            }

            CreateConnectionRequest request = new CreateConnectionRequest();
            request.setName(getStringField(rawRequest, "name", "instance_name",
                    "ServiceNow Connection " + Instant.now().toString().substring(0, 10)));
            request.setInstanceUrl(instanceUrl);
            request.setAuthType(getStringField(rawRequest, "authType", "auth_type", "OAUTH2"));
            request.setCredentialsRef(credentialsRef);

            ConnectionDTO created = connectionConfigService.createConnection(orgId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Failed to create connection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create connection", "detail", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConnection(
            @PathVariable UUID id,
            @RequestBody CreateConnectionRequest request) {
        try {
            return ResponseEntity.ok(connectionConfigService.updateConnection(id, request));
        } catch (Exception e) {
            logger.error("Failed to update connection {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update connection", "detail", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConnection(@PathVariable UUID id) {
        try {
            connectionConfigService.deleteConnection(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Delete connection not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Connection not found", "id", id.toString()));
        } catch (Exception e) {
            logger.error("Failed to delete connection {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete connection", "detail", e.getMessage()));
        }
    }

    // ==================== Test & Sync ====================

    @PostMapping("/test")
    public ResponseEntity<TestConnectionResponse> testConnection(
            @RequestBody(required = false) Map<String, Object> rawRequest) {
        try {
            if (rawRequest == null) rawRequest = Map.of();

            String instanceUrl = getStringField(rawRequest, "instanceUrl", "instance_url", null);
            if (instanceUrl == null || instanceUrl.isBlank()) {
                return ResponseEntity.badRequest().body(new TestConnectionResponse(
                        false, "instanceUrl (or instance_url) is required", null, 0L));
            }

            TestConnectionRequest request = new TestConnectionRequest();
            request.setInstanceUrl(instanceUrl);
            request.setAuthType(getStringField(rawRequest, "authType", "auth_type", "OAUTH2"));
            request.setCredentialsRef(getStringField(rawRequest, "credentialsRef", "credentials_ref",
                    "vault://servicenow/default"));

            TestConnectionResponse response = connectionConfigService.testConnection(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Test connection failed: {}", e.getMessage());
            return ResponseEntity.ok(new TestConnectionResponse(
                    false,
                    "Connection test failed: " + e.getMessage(),
                    null,
                    0L
            ));
        }
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Map<String, Object>> triggerSync(
            @PathVariable UUID id,
            @RequestParam(required = false) String sysUpdatedOnAfter) {
        logger.info("Manual sync triggered for connection: {}", id);
        try {
            DataFetchService.FetchResult result = dataFetchService.fetchAndStore(id, sysUpdatedOnAfter);
            return ResponseEntity.accepted().body(Map.of(
                    "connection_id", id.toString(),
                    "records_fetched", result.getRecordsFetched(),
                    "records_stored", result.getRecordsStored(),
                    "status", "COMPLETED"));
        } catch (Exception e) {
            logger.error("Sync failed for connection {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "connection_id", id.toString(),
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== ITSM – Resolver Groups ====================

    /** List all resolver groups configured for a connection. */
    @GetMapping("/{connectionId}/resolver-groups")
    public ResponseEntity<List<ResolverGroupDto>> listResolverGroups(
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(itsmSyncService.listGroups(connectionId));
    }

    /** Register a new resolver group (assignment_group) to monitor. */
    @PostMapping("/{connectionId}/resolver-groups")
    public ResponseEntity<ResolverGroupDto> createResolverGroup(
            @PathVariable UUID connectionId,
            @RequestHeader(value = "X-Org-Id") UUID orgId,
            @Valid @RequestBody CreateResolverGroupRequest req) {
        ResolverGroupDto created = itsmSyncService.createGroup(connectionId, orgId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Remove a resolver group from monitoring. */
    @DeleteMapping("/{connectionId}/resolver-groups/{groupId}")
    public ResponseEntity<Void> deleteResolverGroup(
            @PathVariable UUID connectionId,
            @PathVariable UUID groupId) {
        itsmSyncService.deleteGroup(connectionId, groupId);
        return ResponseEntity.noContent().build();
    }

    /** Trigger manual ITSM sync for a specific resolver group. */
    @PostMapping("/{connectionId}/resolver-groups/{groupId}/sync")
    public ResponseEntity<Map<String, Object>> syncResolverGroup(
            @PathVariable UUID connectionId,
            @PathVariable UUID groupId,
            @RequestParam(required = false) String incrementalTs) {
        logger.info("ITSM sync triggered: connection={}, group={}", connectionId, groupId);
        try {
            ItsmSyncService.SyncResult result = itsmSyncService.syncGroup(connectionId, groupId, incrementalTs);
            return ResponseEntity.accepted().body(Map.of(
                    "connection_id", connectionId.toString(),
                    "resolver_group_id", groupId.toString(),
                    "records_fetched", result.fetched(),
                    "records_stored", result.stored(),
                    "status", "COMPLETED"));
        } catch (Exception e) {
            logger.error("ITSM sync failed for group {}: {}", groupId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "error", e.getMessage()));
        }
    }

    /** Get aggregated ITSM KPIs for a resolver group (read from stored data). */
    @GetMapping("/{connectionId}/resolver-groups/{groupId}/summary")
    public ResponseEntity<ItsmSummaryDto> getItsmSummary(
            @PathVariable UUID connectionId,
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(itsmSyncService.buildSummary(connectionId, groupId));
    }

    // ==================== Project Sync ====================

    /**
     * Get project-sync configuration for a connection.
     * Returns 404 if no config exists yet.
     */
    @GetMapping("/{connectionId}/project-sync")
    public ResponseEntity<ProjectSyncConfigDto> getProjectSyncConfig(
            @PathVariable UUID connectionId) {
        return projectFetchService.getConfig(connectionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update project-sync configuration for a connection.
     * Idempotent – creates on first call, updates on subsequent calls.
     */
    @PostMapping("/{connectionId}/project-sync")
    public ResponseEntity<ProjectSyncConfigDto> upsertProjectSyncConfig(
            @PathVariable UUID connectionId,
            @RequestHeader(value = "X-Org-Id") UUID orgId,
            @Valid @RequestBody UpsertProjectSyncConfigRequest req) {
        ProjectSyncConfigDto saved = projectFetchService.upsertConfig(connectionId, orgId, req);
        return ResponseEntity.ok(saved);
    }

    /**
     * Trigger a manual project sync for a connection.
     * Fetches pm_project + tasks + budget plans, calculates KPIs and RAG status,
     * and persists to snow_projects / snow_project_tasks / snow_project_budgets.
     */
    @PostMapping("/{connectionId}/project-sync/trigger")
    public ResponseEntity<Map<String, Object>> triggerProjectSync(
            @PathVariable UUID connectionId) {
        logger.info("Project sync triggered for connection: {}", connectionId);
        try {
            ProjectFetchService.SyncResult result = projectFetchService.syncProjects(connectionId);
            return ResponseEntity.accepted().body(Map.of(
                    "connection_id", connectionId.toString(),
                    "projects_fetched", result.fetched(),
                    "projects_stored", result.stored(),
                    "status", "COMPLETED"));
        } catch (Exception e) {
            logger.error("Project sync failed for connection {}: {}", connectionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "error", e.getMessage()));
        }
    }

    // ==================== Health ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-ext-snow"));
    }

    // ==================== Helpers ====================

    private String getStringField(Map<String, Object> map, String camelKey, String snakeKey, String defaultVal) {
        Object val = map.get(camelKey);
        if (val == null) val = map.get(snakeKey);
        return val != null ? val.toString() : defaultVal;
    }
}
