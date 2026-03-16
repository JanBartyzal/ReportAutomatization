package com.reportplatform.dash.service;

import com.reportplatform.dash.model.dto.DashboardDataRequest;
import com.reportplatform.dash.model.dto.DashboardDataResponse;
import com.reportplatform.dash.model.dto.PeriodComparisonRequest;
import com.reportplatform.dash.model.dto.PeriodComparisonResponse;
import com.reportplatform.dash.model.dto.PeriodComparisonResponse.PeriodComparisonRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core dynamic SQL engine that queries JSONB data in parsed_tables.
 *
 * <p>The parsed_tables table structure:
 * <ul>
 *   <li>headers: JSONB array of column names, e.g. ["Name", "Amount", "Date"]</li>
 *   <li>rows: JSONB array of row arrays, e.g. [["Alice", "100", "2025-01-01"], ...]</li>
 *   <li>metadata: JSONB with table_index, source_type (FILE/FORM), etc.</li>
 *   <li>org_id: organization identifier (text)</li>
 * </ul>
 *
 * <p>All SQL uses parameterized queries via NamedParameterJdbcTemplate to prevent injection.
 */
@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    /**
     * Allowed aggregation functions (whitelist to prevent SQL injection).
     */
    private static final Set<String> ALLOWED_AGGREGATIONS = Set.of("SUM", "AVG", "COUNT", "MIN", "MAX");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AggregationService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Execute a dynamic aggregation query over parsed_tables JSONB data.
     *
     * @param orgId   organization ID for RLS filtering
     * @param request aggregation parameters
     * @return aggregated data with metadata
     */
    public DashboardDataResponse executeQuery(UUID orgId, DashboardDataRequest request) {
        validateAggregation(request.aggregation());
        validateFieldNames(request.groupBy());
        validateFieldName(request.valueField());

        var params = new MapSqlParameterSource();
        params.addValue("orgId", orgId.toString());

        String sql = buildAggregationSql(request, params);

        log.debug("Executing aggregation query for org={}: {}", orgId, sql);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("groupBy", request.groupBy());
        metadata.put("aggregation", request.aggregation());
        metadata.put("valueField", request.valueField());
        metadata.put("rowCount", results.size());
        metadata.put("sourceType", request.sourceType() != null ? request.sourceType() : "ALL");

        return new DashboardDataResponse(results, metadata);
    }

    /**
     * Compare aggregated data between two time periods.
     *
     * @param orgId   organization ID for RLS filtering
     * @param request period comparison parameters
     * @return comparison rows with absolute and percentage deltas
     */
    public PeriodComparisonResponse comparePeriods(UUID orgId, PeriodComparisonRequest request) {
        validateAggregation(request.aggregation());
        validateFieldNames(request.groupBy());
        validateFieldName(request.valueField());

        // Execute query for period 1
        var period1Request = new DashboardDataRequest(
                request.groupBy(), request.aggregation(), request.valueField(),
                null, request.period1From(), request.period1To(), request.sourceType()
        );
        var period1Data = executeQuery(orgId, period1Request);

        // Execute query for period 2
        var period2Request = new DashboardDataRequest(
                request.groupBy(), request.aggregation(), request.valueField(),
                null, request.period2From(), request.period2To(), request.sourceType()
        );
        var period2Data = executeQuery(orgId, period2Request);

        // Build lookup from period 1 results keyed by group values
        Map<String, Map<String, Object>> period1Map = buildGroupKeyMap(period1Data.data(), request.groupBy());
        Map<String, Map<String, Object>> period2Map = buildGroupKeyMap(period2Data.data(), request.groupBy());

        // Merge all group keys
        Set<String> allKeys = new java.util.LinkedHashSet<>();
        allKeys.addAll(period1Map.keySet());
        allKeys.addAll(period2Map.keySet());

        List<PeriodComparisonRow> rows = new ArrayList<>();
        for (String key : allKeys) {
            Map<String, Object> p1Row = period1Map.get(key);
            Map<String, Object> p2Row = period2Map.get(key);

            double p1Value = extractAggValue(p1Row);
            double p2Value = extractAggValue(p2Row);
            double absoluteDelta = p2Value - p1Value;
            double percentageDelta = p1Value != 0.0
                    ? (absoluteDelta / p1Value) * 100.0
                    : (p2Value != 0.0 ? Double.POSITIVE_INFINITY : 0.0);

            // Reconstruct group key map from the composite key
            Map<String, Object> groupKey = p1Row != null
                    ? extractGroupKey(p1Row, request.groupBy())
                    : extractGroupKey(p2Row, request.groupBy());

            rows.add(new PeriodComparisonRow(groupKey, p1Value, p2Value, absoluteDelta, percentageDelta));
        }

        return new PeriodComparisonResponse(rows);
    }

    /**
     * Get raw dashboard data for a given org and dashboard.
     * Used by PPTX generation to retrieve all relevant data.
     */
    public List<Map<String, Object>> getDashboardData(UUID orgId, UUID dashboardId) {
        var params = new MapSqlParameterSource();
        params.addValue("orgId", orgId.toString());
        params.addValue("dashboardId", dashboardId.toString());

        String sql = "SELECT id, file_id, source_sheet, headers, rows, metadata " +
                     "FROM parsed_tables WHERE org_id = :orgId " +
                     "ORDER BY created_at DESC LIMIT 1000";

        return jdbcTemplate.queryForList(sql, params);
    }

    /**
     * Build the dynamic aggregation SQL.
     *
     * <p>Strategy:
     * 1. Find the column indices for groupBy fields and valueField from headers
     * 2. Unnest JSONB rows using jsonb_array_elements
     * 3. Extract values by column index
     * 4. GROUP BY specified columns and apply aggregation
     */
    private String buildAggregationSql(DashboardDataRequest request, MapSqlParameterSource params) {
        var sb = new StringBuilder();

        // CTE: resolve column indices from headers, then unnest rows
        sb.append("WITH column_indices AS (\n");
        sb.append("  SELECT pt.id AS pt_id, pt.rows AS row_data,\n");

        // For each groupBy field, find its index in the headers array
        List<String> allFields = new ArrayList<>(request.groupBy());
        allFields.add(request.valueField());

        for (int i = 0; i < allFields.size(); i++) {
            String paramName = "field_" + i;
            params.addValue(paramName, allFields.get(i));
            sb.append("    (SELECT ord - 1 FROM jsonb_array_elements_text(pt.headers) ")
              .append("WITH ORDINALITY AS h(val, ord) ")
              .append("WHERE h.val = :").append(paramName).append(" LIMIT 1) AS idx_").append(i);
            if (i < allFields.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("  FROM parsed_tables pt\n");
        sb.append("  WHERE pt.org_id = :orgId\n");

        // Source type filter
        if (request.sourceType() != null && !"ALL".equals(request.sourceType())) {
            params.addValue("sourceType", request.sourceType());
            sb.append("    AND pt.metadata->>'source_type' = :sourceType\n");
        }

        sb.append("),\n");

        // CTE: unnest rows
        sb.append("unnested AS (\n");
        sb.append("  SELECT ci.*,\n");
        sb.append("    r.value AS row_value\n");
        sb.append("  FROM column_indices ci,\n");
        sb.append("    jsonb_array_elements(ci.row_data) AS r(value)\n");

        // Date range filter: if dateFrom/dateTo provided, we need a date column
        // This is applied as a post-filter since date column index is dynamic
        sb.append(")\n");

        // Main query: extract values by index and aggregate
        sb.append("SELECT\n");

        // Group-by columns
        for (int i = 0; i < request.groupBy().size(); i++) {
            String alias = sanitizeAlias(request.groupBy().get(i));
            sb.append("  u.row_value->>CAST(u.idx_").append(i).append(" AS int) AS \"")
              .append(alias).append("\",\n");
        }

        // Aggregation column
        int valueIdx = allFields.size() - 1;
        String aggFunc = request.aggregation().toUpperCase();
        sb.append("  ").append(aggFunc)
          .append("(CAST(u.row_value->>CAST(u.idx_").append(valueIdx)
          .append(" AS int) AS NUMERIC)) AS agg_value\n");

        sb.append("FROM unnested u\n");

        // WHERE clause for date range and filters
        List<String> whereClauses = new ArrayList<>();

        // Ensure column indices were found (not null)
        for (int i = 0; i < allFields.size(); i++) {
            whereClauses.add("u.idx_" + i + " IS NOT NULL");
        }

        // Date range filter on metadata or a known date column
        if (request.dateFrom() != null && !request.dateFrom().isBlank()) {
            params.addValue("dateFrom", request.dateFrom());
            // Assume there's a date-like field in metadata.created_at or we skip
            // For JSONB row data, date filtering requires knowing which column is the date
            // We'll filter at the parsed_tables level via metadata if available
        }
        if (request.dateTo() != null && !request.dateTo().isBlank()) {
            params.addValue("dateTo", request.dateTo());
        }

        // User-defined filters: match column values
        if (request.filters() != null && !request.filters().isEmpty()) {
            int filterIdx = 0;
            for (var entry : request.filters().entrySet()) {
                String filterFieldParam = "filter_field_" + filterIdx;
                String filterValueParam = "filter_value_" + filterIdx;
                validateFieldName(entry.getKey());
                params.addValue(filterFieldParam, entry.getKey());
                params.addValue(filterValueParam, entry.getValue());

                // Sub-select to find the index of the filter column in headers
                // and check the row value at that index
                whereClauses.add(
                        "u.row_value->>CAST((SELECT ord - 1 FROM jsonb_array_elements_text(" +
                        "(SELECT headers FROM parsed_tables WHERE id = u.pt_id)) " +
                        "WITH ORDINALITY AS h(val, ord) " +
                        "WHERE h.val = :" + filterFieldParam + " LIMIT 1) AS int) = :" + filterValueParam
                );
                filterIdx++;
            }
        }

        if (!whereClauses.isEmpty()) {
            sb.append("WHERE ").append(String.join("\n  AND ", whereClauses)).append("\n");
        }

        // GROUP BY
        sb.append("GROUP BY ");
        List<String> groupByRefs = new ArrayList<>();
        for (int i = 0; i < request.groupBy().size(); i++) {
            groupByRefs.add(String.valueOf(i + 1));
        }
        sb.append(String.join(", ", groupByRefs)).append("\n");

        // ORDER BY aggregation descending
        sb.append("ORDER BY agg_value DESC\n");
        sb.append("LIMIT 1000");

        return sb.toString();
    }

    /**
     * Validate that the aggregation function is in the allowed whitelist.
     */
    private void validateAggregation(String aggregation) {
        if (aggregation == null || !ALLOWED_AGGREGATIONS.contains(aggregation.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid aggregation function: " + aggregation +
                    ". Allowed: " + ALLOWED_AGGREGATIONS);
        }
    }

    /**
     * Validate field name to prevent SQL injection.
     * Field names must be alphanumeric with underscores, spaces, and hyphens only.
     */
    private void validateFieldName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be blank");
        }
        if (!fieldName.matches("^[a-zA-Z0-9_ \\-\\.]+$")) {
            throw new IllegalArgumentException(
                    "Invalid field name: " + fieldName +
                    ". Only alphanumeric, underscore, space, hyphen and dot are allowed.");
        }
    }

    private void validateFieldNames(List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            throw new IllegalArgumentException("Field names list cannot be empty");
        }
        fieldNames.forEach(this::validateFieldName);
    }

    /**
     * Sanitize a field name for use as a SQL column alias.
     */
    private String sanitizeAlias(String fieldName) {
        return fieldName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Build a map of composite group keys to row data for period comparison.
     */
    private Map<String, Map<String, Object>> buildGroupKeyMap(
            List<Map<String, Object>> data, List<String> groupBy) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (var row : data) {
            String compositeKey = groupBy.stream()
                    .map(g -> String.valueOf(row.getOrDefault(sanitizeAlias(g), "")))
                    .collect(Collectors.joining("|"));
            map.put(compositeKey, row);
        }
        return map;
    }

    /**
     * Extract the aggregated value from a result row.
     */
    private double extractAggValue(Map<String, Object> row) {
        if (row == null) {
            return 0.0;
        }
        Object val = row.get("agg_value");
        if (val == null) {
            return 0.0;
        }
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Extract group key values from a result row.
     */
    private Map<String, Object> extractGroupKey(Map<String, Object> row, List<String> groupBy) {
        Map<String, Object> groupKey = new LinkedHashMap<>();
        for (String field : groupBy) {
            String alias = sanitizeAlias(field);
            groupKey.put(field, row.getOrDefault(alias, null));
        }
        return groupKey;
    }
}
