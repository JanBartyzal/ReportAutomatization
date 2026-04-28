package com.reportplatform.ver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reportplatform.ver.model.dto.CreateVersionRequest;
import com.reportplatform.ver.model.dto.VersionDiffResponse;
import com.reportplatform.ver.model.dto.VersionResponse;
import com.reportplatform.ver.service.VersionDiffService;
import com.reportplatform.ver.service.VersionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
public class VersionController {

    private static final Logger logger = LoggerFactory.getLogger(VersionController.class);

    private final VersionService versionService;
    private final VersionDiffService versionDiffService;
    private final ObjectMapper objectMapper;

    public VersionController(VersionService versionService,
                             VersionDiffService versionDiffService,
                             ObjectMapper objectMapper) {
        this.versionService = versionService;
        this.versionDiffService = versionDiffService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> listVersions(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(required = false) Integer version) {
        try {
            if (version != null) {
                try {
                    var v = versionService.getVersionEntity(entityType, entityId, version);
                    return ResponseEntity.ok(List.of(VersionResponse.from(v)));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.notFound().build();
                }
            }
            return ResponseEntity.ok(versionService.listVersions(entityType, entityId));
        } catch (Exception e) {
            logger.error("Failed to list versions for {}/{}: {}", entityType, entityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list versions", "detail", e.getMessage()));
        }
    }

    @GetMapping("/{entityType}/{entityId}/diff")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> getDiff(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam int v1,
            @RequestParam int v2) {
        try {
            return ResponseEntity.ok(versionDiffService.getDiff(entityType, entityId, v1, v2));
        } catch (IllegalArgumentException e) {
            logger.warn("getDiff: versions not found for {}/{} v{}..v{}: {}", entityType, entityId, v1, v2, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "One or both versions not found",
                    "entityType", entityType,
                    "entityId", entityId.toString(),
                    "fromVersion", v1,
                    "toVersion", v2,
                    "detail", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("getDiff failed for {}/{} v{}..v{}: {}", entityType, entityId, v1, v2, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Diff computation failed",
                    "detail", e.getMessage()
            ));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<VersionResponse> createVersion(
            @RequestBody @Valid CreateVersionRequest request,
            @RequestHeader("X-Org-Id") UUID orgId) {

        boolean isLocked = versionService.isLatestVersionLocked(
                request.entityType(), request.entityId());

        VersionResponse response;
        if (isLocked) {
            response = versionService.createVersionOnLockedEntity(request, orgId);
        } else {
            response = versionService.createVersion(request, orgId);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Convenience endpoint: POST /api/versions/{entityType}/{entityId}
     * Accepts simplified body: {"comment": "..."} instead of full CreateVersionRequest
     */
    @PostMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> createVersionForEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            String comment = body != null ? String.valueOf(body.getOrDefault("comment", "")) : "";

            // Build a minimal snapshot with the comment
            ObjectNode snapshot = objectMapper.createObjectNode();
            snapshot.put("comment", comment);
            snapshot.put("entity_type", entityType);
            snapshot.put("entity_id", entityId.toString());

            if (orgId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "X-Org-Id header is required"));
            }
            UUID effectiveOrgId = orgId;

            CreateVersionRequest request = new CreateVersionRequest(
                    entityType, entityId, snapshot, comment, userId);

            boolean isLocked = false;
            try {
                isLocked = versionService.isLatestVersionLocked(entityType, entityId);
            } catch (Exception e) {
                // No previous versions exist — treat as unlocked
            }

            VersionResponse response;
            if (isLocked) {
                response = versionService.createVersionOnLockedEntity(request, effectiveOrgId);
            } else {
                response = versionService.createVersion(request, effectiveOrgId);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Failed to create version for {}/{}: {}", entityType, entityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create version", "detail", e.getMessage()));
        }
    }

    /**
     * PUT /api/versions/{entityType}/{entityId} → 405 Method Not Allowed
     * Versions are immutable once created.
     */
    @PutMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> immutableVersionCheck(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "Versions are immutable and cannot be modified",
                             "entity_type", entityType,
                             "entity_id", entityId.toString()));
    }
}
