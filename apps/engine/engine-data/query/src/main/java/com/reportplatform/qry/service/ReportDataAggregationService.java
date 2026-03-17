package com.reportplatform.qry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for aggregating report data for PPTX generation.
 * Combines form responses and uploaded file data, applies placeholder mapping,
 * and provides data in the format required by MS-GEN-PPTX.
 */
@Service
public class ReportDataAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(ReportDataAggregationService.class);
    private static final String CACHE_PREFIX = "report_data:";
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;
    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;

    @Value("${dapr.remote.ms-form-app-id:ms-form}")
    private String msFormAppId;

    @Value("${dapr.remote.ms-sink-tbl-app-id:ms-sink-tbl}")
    private String msSinkTblAppId;

    @Value("${dapr.remote.ms-tmpl-pptx-app-id:ms-tmpl-pptx}")
    private String msTmplPptxAppId;

    public ReportDataAggregationService(RedisTemplate<String, Object> redisTemplate,
            CacheService cacheService,
            DaprClient daprClient) {
        this.redisTemplate = redisTemplate;
        this.cacheService = cacheService;
        this.daprClient = daprClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get aggregated data for a report.
     * Uses caching to improve performance.
     */
    public AggregatedReportData getAggregatedData(String reportId, String orgId, String templateId) {
        String cacheKey = CACHE_PREFIX + reportId;

        // Try to get from cache first
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof AggregatedReportData) {
                logger.debug("Cache hit for report {}", reportId);
                return (AggregatedReportData) cached;
            }
        } catch (Exception e) {
            logger.warn("Cache read failed for report {}: {}", reportId, e.getMessage());
        }

        // Cache miss - fetch and aggregate data
        logger.info("Aggregating data for report {}", reportId);
        AggregatedReportData data = aggregateReportData(reportId, orgId, templateId);

        // Store in cache
        try {
            redisTemplate.opsForValue().set(cacheKey, data, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
            logger.debug("Cached data for report {}", reportId);
        } catch (Exception e) {
            logger.warn("Cache write failed for report {}: {}", reportId, e.getMessage());
        }

        return data;
    }

    /**
     * Aggregate report data from form responses and uploaded files.
     * Integrates with MS-FORM, MS-SINK-TBL, and MS-TMPL-PPTX services.
     */
    private AggregatedReportData aggregateReportData(String reportId, String orgId, String templateId) {
        Map<String, String> textPlaceholders = new HashMap<>();
        List<TableData> tables = new ArrayList<>();
        List<ChartData> charts = new ArrayList<>();
        List<String> availableFields = new ArrayList<>();

        // Fetch data from MS-FORM (form responses)
        try {
            Map<String, Object> formData = fetchFormData(reportId, orgId);
            if (formData != null && !formData.isEmpty()) {
                // Extract text placeholders from form data
                for (Map.Entry<String, Object> entry : formData.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        textPlaceholders.put(entry.getKey(), (String) entry.getValue());
                        availableFields.add(entry.getKey());
                    }
                }
                logger.info("Fetched {} text placeholders from MS-FORM", textPlaceholders.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch form data from MS-FORM: {}", e.getMessage());
        }

        // Fetch data from MS-SINK-TBL (uploaded file data)
        try {
            List<TableData> fileTables = fetchTableData(reportId, orgId);
            tables.addAll(fileTables);
            for (TableData table : fileTables) {
                availableFields.add(table.placeholderKey());
            }
            logger.info("Fetched {} tables from MS-SINK-TBL", fileTables.size());
        } catch (Exception e) {
            logger.warn("Failed to fetch table data from MS-SINK-TBL: {}", e.getMessage());
        }

        // Fetch placeholder mapping from MS-TMPL-PPTX
        try {
            Map<String, String> mappings = fetchPlaceholderMappings(templateId, orgId);
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                textPlaceholders.putIfAbsent(entry.getKey(), entry.getValue());
                availableFields.add(entry.getKey());
            }
            logger.info("Fetched {} placeholder mappings from MS-TMPL-PPTX", mappings.size());
        } catch (Exception e) {
            logger.warn("Failed to fetch placeholder mappings from MS-TMPL-PPTX: {}", e.getMessage());
        }

        // Log warning if no data was retrieved from any upstream service
        if (textPlaceholders.isEmpty() && tables.isEmpty() && charts.isEmpty()) {
            logger.warn("No data from any upstream service for report {}. Returning empty aggregation.", reportId);
        }

        return new AggregatedReportData(
                reportId,
                textPlaceholders,
                tables,
                charts,
                availableFields,
                Instant.now().toEpochMilli());
    }

    /**
     * Fetch form data from MS-FORM service.
     */
    private Map<String, Object> fetchFormData(String reportId, String orgId) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("orgId", orgId);
            request.put("reportId", reportId);

            Map<String, Object> response = daprClient.invokeMethod(
                    msFormAppId,
                    "/api/reports/" + reportId + "/data",
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            return response;
        } catch (Exception e) {
            logger.debug("MS-FORM call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch table data from MS-SINK-TBL service.
     */
    private List<TableData> fetchTableData(String reportId, String orgId) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("orgId", orgId);
            request.put("reportId", reportId);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = (List<Map<String, Object>>) daprClient.invokeMethod(
                    msSinkTblAppId,
                    "/api/tables/report/" + reportId,
                    request,
                    HttpExtension.POST,
                    new TypeRef<List<Map<String, Object>>>() {
                    })
                    .block();

            if (response == null) {
                return Collections.emptyList();
            }

            List<TableData> tables = new ArrayList<>();
            for (Map<String, Object> tableMap : response) {
                String placeholderKey = (String) tableMap.getOrDefault("placeholderKey", "TABLE:" + tableMap.get("id"));
                List<String> headers = objectMapper.convertValue(
                        tableMap.get("headers"),
                        new TypeReference<List<String>>() {
                        });
                List<List<String>> rows = objectMapper.convertValue(
                        tableMap.get("rows"),
                        new TypeReference<List<List<String>>>() {
                        });

                tables.add(new TableData(
                        placeholderKey,
                        headers,
                        rows,
                        TableAggregationType.DETAIL));
            }

            return tables;
        } catch (Exception e) {
            logger.debug("MS-SINK-TBL call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch placeholder mappings from MS-TMPL-PPTX service.
     */
    private Map<String, String> fetchPlaceholderMappings(String templateId, String orgId) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("orgId", orgId);

            @SuppressWarnings("unchecked")
            Map<String, String> response = (Map<String, String>) daprClient.invokeMethod(
                    msTmplPptxAppId,
                    "/api/templates/" + templateId + "/placeholders",
                    request,
                    HttpExtension.POST,
                    new TypeRef<Map<String, String>>() {
                    })
                    .block();

            return response != null ? response : Collections.emptyMap();
        } catch (Exception e) {
            logger.debug("MS-TMPL-PPTX call failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Invalidate cached data for a report.
     * Call this when report data changes.
     */
    public void invalidateCache(String reportId) {
        String cacheKey = CACHE_PREFIX + reportId;
        try {
            redisTemplate.delete(cacheKey);
            logger.info("Invalidated cache for report {}", reportId);
        } catch (Exception e) {
            logger.warn("Cache invalidation failed for report {}: {}", reportId, e.getMessage());
        }
    }

    // ========== Data Classes ==========

    public record AggregatedReportData(
            String reportId,
            Map<String, String> textPlaceholders,
            List<TableData> tables,
            List<ChartData> charts,
            List<String> availableFields,
            long cachedAt) {
    }

    public record TableData(
            String placeholderKey,
            List<String> headers,
            List<List<String>> rows,
            TableAggregationType aggregationType) {
    }

    public record ChartData(
            String placeholderKey,
            String chartType,
            List<String> labels,
            List<ChartSeries> series,
            ChartAggregationType aggregationType) {
    }

    public record ChartSeries(
            String name,
            List<Double> values) {
    }

    public enum TableAggregationType {
        NONE,
        SUM,
        AVG,
        DETAIL
    }

    public enum ChartAggregationType {
        NONE,
        SUM,
        AVG,
        CUMULATIVE
    }
}
