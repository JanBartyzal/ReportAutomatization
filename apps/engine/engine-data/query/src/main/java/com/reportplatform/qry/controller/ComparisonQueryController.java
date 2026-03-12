package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.dto.ComparisonResult;
import com.reportplatform.qry.service.ComparisonQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for multi-org comparison queries.
 * Restricted to HOLDING_ADMIN role.
 */
@RestController
@RequestMapping("/api/query")
public class ComparisonQueryController {

    private static final String HOLDING_ADMIN = "HOLDING_ADMIN";

    private final ComparisonQueryService comparisonQueryService;

    public ComparisonQueryController(ComparisonQueryService comparisonQueryService) {
        this.comparisonQueryService = comparisonQueryService;
    }

    /**
     * Compare a metric across multiple organizations.
     * Requires HOLDING_ADMIN role.
     *
     * @param orgIds  comma-separated list of org IDs
     * @param metric  metric name (file_count, total_size_bytes, clean_count)
     * @param scope   scope filter (CENTRAL, LOCAL, ALL)
     */
    @GetMapping("/comparison")
    public ResponseEntity<ComparisonResult> getComparison(
            @RequestHeader(value = "X-User-Role") String userRole,
            @RequestParam(value = "org_ids") List<String> orgIds,
            @RequestParam(value = "metric") String metric,
            @RequestParam(value = "scope", defaultValue = "ALL") String scope) {

        if (!HOLDING_ADMIN.equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (orgIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ComparisonResult result = comparisonQueryService.getMultiOrgComparison(orgIds, metric, scope);
        return ResponseEntity.ok(result);
    }
}
