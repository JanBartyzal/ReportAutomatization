package com.reportplatform.enginedata.controller;

import com.reportplatform.sink.tbl.entity.StorageRoutingConfigEntity;
import com.reportplatform.sink.tbl.repository.StorageRoutingConfigRepository;
import com.reportplatform.sink.tbl.service.StorageRoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin REST API for managing storage routing rules.
 * <p>
 * Allows a HoldingAdmin to route specific organisations or source types
 * to the SPARK backend without a service restart.
 * </p>
 *
 * <p>Base path: {@code /api/v1/admin/storage-routing}</p>
 */
@RestController
@RequestMapping("/api/v1/admin/storage-routing")
public class StorageRoutingAdminController {

    private final StorageRoutingConfigRepository repository;
    private final StorageRoutingService routingService;

    public StorageRoutingAdminController(
            StorageRoutingConfigRepository repository,
            StorageRoutingService routingService) {
        this.repository = repository;
        this.routingService = routingService;
    }

    /**
     * List all routing rules currently in the database.
     *
     * @return ordered list of rules (most specific first)
     */
    @GetMapping
    public List<StorageRoutingConfigEntity> listRules() {
        return repository.findAllEffective(OffsetDateTime.now());
    }

    /**
     * Upsert a routing rule.
     * <p>
     * If a rule for the same (orgId, sourceType) combination already exists
     * it is replaced; otherwise a new rule is created.
     * </p>
     *
     * @param request rule definition
     * @return the saved rule
     */
    @PutMapping
    public ResponseEntity<StorageRoutingConfigEntity> upsertRule(@RequestBody UpsertRuleRequest request) {
        validateBackend(request.backend());

        // Check for existing rule with same discriminators
        var existing = repository.findAllEffective(OffsetDateTime.now()).stream()
                .filter(r -> equalsNullable(r.getOrgId(), parseUuid(request.orgId()))
                        && equalsNullable(r.getSourceType(), request.sourceType()))
                .findFirst();

        StorageRoutingConfigEntity entity = existing.orElseGet(StorageRoutingConfigEntity::new);
        entity.setOrgId(parseUuid(request.orgId()));
        entity.setSourceType(request.sourceType());
        entity.setBackend(request.backend());
        entity.setEffectiveFrom(
                request.effectiveFrom() != null ? request.effectiveFrom() : OffsetDateTime.now());
        entity.setCreatedBy(request.createdBy());

        entity = repository.save(entity);
        routingService.refreshRules();

        return ResponseEntity.ok(entity);
    }

    /**
     * Delete a routing rule by ID.
     * The global default rule (both discriminators NULL) cannot be deleted.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        var rule = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        if (rule.getOrgId() == null && rule.getSourceType() == null) {
            return ResponseEntity.badRequest().build(); // protect global default
        }

        repository.deleteById(id);
        routingService.refreshRules();
        return ResponseEntity.noContent().build();
    }

    /**
     * Force immediate refresh of the in-memory rule cache.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh() {
        routingService.refreshRules();
        return ResponseEntity.noContent().build();
    }

    // --- helpers ---

    private void validateBackend(String backend) {
        if (!"POSTGRES".equals(backend) && !"SPARK".equals(backend) && !"BLOB".equals(backend)) {
            throw new IllegalArgumentException("backend must be POSTGRES, SPARK, or BLOB, got: " + backend);
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        return UUID.fromString(value);
    }

    private static boolean equalsNullable(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Request body for upsert endpoint.
     *
     * @param orgId         UUID string; null = wildcard (all orgs)
     * @param sourceType    e.g. "EXCEL", "SERVICE_NOW"; null = all types
     * @param backend       "POSTGRES", "SPARK", or "BLOB"
     * @param effectiveFrom ISO-8601 timestamp; null = now
     * @param createdBy     audit: who created the rule
     */
    public record UpsertRuleRequest(
            String orgId,
            String sourceType,
            String backend,
            OffsetDateTime effectiveFrom,
            String createdBy) {
    }
}
