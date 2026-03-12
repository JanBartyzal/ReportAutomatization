package com.reportplatform.qry.service;

import com.reportplatform.qry.model.dto.ComparisonResult;
import com.reportplatform.qry.model.dto.OrgMetricValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for multi-org comparison queries.
 * Used by HoldingAdmin to compare metrics across organizations.
 * Requires HOLDING_ADMIN role (RLS policies allow cross-org access).
 */
@Service
public class ComparisonQueryService {

    private static final Logger log = LoggerFactory.getLogger(ComparisonQueryService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final CacheService cacheService;

    public ComparisonQueryService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Compares a metric across multiple organizations for a given period.
     * Uses the mv_scope_summary materialized view for aggregated data.
     *
     * @param orgIds   list of organization IDs to compare
     * @param metric   the metric to compare (e.g. "file_count", "total_size_bytes")
     * @param scope    scope filter (CENTRAL, LOCAL, ALL)
     * @return comparison result with per-org values
     */
    @Transactional(readOnly = true)
    public ComparisonResult getMultiOrgComparison(List<String> orgIds, String metric, String scope) {
        String cacheKey = "qry:comparison:" + String.join(",", orgIds) + ":" + metric + ":" + scope;
        Optional<ComparisonResult> cached = cacheService.getCached(cacheKey, ComparisonResult.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for comparison query");
            return cached.get();
        }

        String metricColumn = resolveMetricColumn(metric);

        String scopeFilter = "ALL".equals(scope) ? "" : " AND scope = :scope";
        String sql = "SELECT org_id, scope, " + metricColumn +
                " FROM mv_scope_summary WHERE org_id IN (:orgIds)" + scopeFilter +
                " ORDER BY org_id";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("orgIds", orgIds);
        if (!"ALL".equals(scope)) {
            query.setParameter("scope", scope);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<OrgMetricValue> values = rows.stream()
                .map(row -> new OrgMetricValue(
                        (String) row[0],
                        (String) row[0], // orgName falls back to orgId; resolved by frontend
                        ((Number) row[2]).doubleValue(),
                        (String) row[1]
                ))
                .collect(Collectors.toList());

        ComparisonResult result = new ComparisonResult(metric, null, values);
        cacheService.putCache(cacheKey, result);

        log.info("Comparison query: metric={}, orgs={}, scope={}, results={}",
                metric, orgIds.size(), scope, values.size());
        return result;
    }

    private String resolveMetricColumn(String metric) {
        return switch (metric) {
            case "file_count" -> "file_count";
            case "total_size_bytes" -> "total_size_bytes";
            case "clean_count" -> "clean_count";
            default -> throw new IllegalArgumentException("Unknown metric: " + metric);
        };
    }
}
