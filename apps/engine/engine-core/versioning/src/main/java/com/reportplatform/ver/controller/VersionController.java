package com.reportplatform.ver.controller;

import com.reportplatform.ver.model.dto.CreateVersionRequest;
import com.reportplatform.ver.model.dto.VersionDiffResponse;
import com.reportplatform.ver.model.dto.VersionResponse;
import com.reportplatform.ver.service.VersionDiffService;
import com.reportplatform.ver.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
public class VersionController {

    private final VersionService versionService;
    private final VersionDiffService versionDiffService;

    public VersionController(VersionService versionService,
                             VersionDiffService versionDiffService) {
        this.versionService = versionService;
        this.versionDiffService = versionDiffService;
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<VersionResponse>> listVersions(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(versionService.listVersions(entityType, entityId));
    }

    @GetMapping("/{entityType}/{entityId}/diff")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<VersionDiffResponse> getDiff(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam int v1,
            @RequestParam int v2) {
        return ResponseEntity.ok(versionDiffService.getDiff(entityType, entityId, v1, v2));
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
}
