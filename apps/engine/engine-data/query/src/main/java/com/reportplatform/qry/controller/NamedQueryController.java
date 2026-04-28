package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.dto.*;
import com.reportplatform.qry.service.NamedQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * REST endpoint for the Named Query Catalog.
 * <p>
 * Org identity is propagated via {@code X-Org-Id} and {@code X-User-Id} headers
 * injected by the Nginx API Gateway after JWT validation – matching the pattern
 * used throughout engine-data controllers.
 */
@RestController
@RequestMapping("/api/v1/data/named-queries")
public class NamedQueryController {

    private final NamedQueryService service;

    public NamedQueryController(NamedQueryService service) {
        this.service = service;
    }

    /** List all accessible named queries (org-scoped + system). Optional filter by data_source_hint. */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<NamedQueryDto> list(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(required = false) String dataSourceHint) {
        return service.listAccessible(UUID.fromString(orgId), dataSourceHint);
    }

    /** Get single named query by ID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<NamedQueryDto> getById(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {
        return service.findById(UUID.fromString(orgId), id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get the params_schema for the query (frontend uses this to render a parameter form). */
    @GetMapping("/{id}/schema")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<String> getSchema(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {
        return service.findById(UUID.fromString(orgId), id)
                .map(q -> ResponseEntity.ok(q.paramsSchema()))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Create a new named query (admin/editor only). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public NamedQueryDto create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateNamedQueryRequest req) {
        return service.create(UUID.fromString(orgId), userId, req);
    }

    /** Partially update a named query. System queries cannot be modified. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public NamedQueryDto update(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNamedQueryRequest req) {
        return service.update(UUID.fromString(orgId), id, req);
    }

    /** Soft-delete a named query. System queries cannot be deleted. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public void delete(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {
        service.delete(UUID.fromString(orgId), id);
    }

    /** Execute a named query with runtime parameters. Returns rows as JSON array. */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public NamedQueryResultDto execute(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id,
            @RequestBody(required = false) NamedQueryExecuteRequest req) {
        NamedQueryExecuteRequest safeReq = req != null ? req : new NamedQueryExecuteRequest(null, null);
        return service.execute(UUID.fromString(orgId), id, safeReq);
    }

    // ---- Exception handlers ----

    /** Malformed X-Org-Id header or invalid UUID path variable → 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", "Bad request", "detail", ex.getMessage());
    }

    /** Query not found (wrong org or unknown id) → 404. */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException ex) {
        return Map.of("error", "Not found", "detail", ex.getMessage());
    }

    /** Attempt to modify/delete a system query or execute an inactive one → 409. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(IllegalStateException ex) {
        return Map.of("error", "Conflict", "detail", ex.getMessage());
    }
}
