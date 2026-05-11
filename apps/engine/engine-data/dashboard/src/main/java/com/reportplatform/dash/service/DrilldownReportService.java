package com.reportplatform.dash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.dash.model.DrilldownReportDefinitionEntity;
import com.reportplatform.dash.model.DrilldownReportSectionEntity;
import com.reportplatform.dash.model.DrilldownReportViewEntity;
import com.reportplatform.dash.model.dto.DashboardDataRequest;
import com.reportplatform.dash.model.dto.DrilldownReportDrillRequest;
import com.reportplatform.dash.model.dto.DrilldownReportListResponse;
import com.reportplatform.dash.model.dto.DrilldownReportQueryRequest;
import com.reportplatform.dash.model.dto.DrilldownReportQueryResponse;
import com.reportplatform.dash.model.dto.DrilldownReportRequest;
import com.reportplatform.dash.model.dto.DrilldownReportResponse;
import com.reportplatform.dash.model.dto.DrilldownReportSectionRequest;
import com.reportplatform.dash.model.dto.DrilldownReportSectionResponse;
import com.reportplatform.dash.model.dto.DrilldownReportViewStateRequest;
import com.reportplatform.dash.model.dto.DrilldownReportViewStateResponse;
import com.reportplatform.dash.repository.DrilldownReportDefinitionRepository;
import com.reportplatform.dash.repository.DrilldownReportSectionRepository;
import com.reportplatform.dash.repository.DrilldownReportViewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DrilldownReportService {

    private static final Logger log = LoggerFactory.getLogger(DrilldownReportService.class);
    private static final int DEFAULT_DRILL_SIZE = 50;
    private static final int MAX_DRILL_SIZE = 500;
    private static final UUID SYSTEM_USER_ID = new UUID(0L, 0L);

    @PersistenceContext
    private EntityManager entityManager;

    private final DrilldownReportDefinitionRepository definitionRepository;
    private final DrilldownReportSectionRepository sectionRepository;
    private final DrilldownReportViewRepository viewRepository;
    private final AggregationService aggregationService;
    private final ObjectMapper objectMapper;

    public DrilldownReportService(
            DrilldownReportDefinitionRepository definitionRepository,
            DrilldownReportSectionRepository sectionRepository,
            DrilldownReportViewRepository viewRepository,
            AggregationService aggregationService,
            ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.sectionRepository = sectionRepository;
        this.viewRepository = viewRepository;
        this.aggregationService = aggregationService;
        this.objectMapper = objectMapper;
    }

    private void setRlsContext(UUID orgId) {
        if (orgId == null) return;
        entityManager.createNativeQuery("SELECT set_config('app.current_org_id', :orgId, true)")
                .setParameter("orgId", orgId.toString())
                .getSingleResult();
    }

    @Transactional(readOnly = true)
    public DrilldownReportListResponse listReports(UUID orgId, UUID userId) {
        if (orgId == null) {
            return new DrilldownReportListResponse(List.of());
        }
        setRlsContext(orgId);
        var reports = definitionRepository.findAccessibleReports(orgId, userId).stream()
                .map(report -> toResponse(report, List.of()))
                .toList();
        return new DrilldownReportListResponse(reports);
    }

    @Transactional
    public DrilldownReportResponse createReport(UUID orgId, UUID userId, DrilldownReportRequest request) {
        requireOrg(orgId);
        setRlsContext(orgId);

        var entity = new DrilldownReportDefinitionEntity();
        applyRequest(entity, orgId, userId, request);
        var saved = definitionRepository.save(entity);

        var sections = saveSections(saved.getId(), orgId, request.sections());
        log.info("Created drilldown report id={} org={} sections={}", saved.getId(), orgId, sections.size());
        return toResponse(saved, sections);
    }

    @Transactional(readOnly = true)
    public DrilldownReportResponse getReport(UUID id, UUID orgId) {
        requireOrg(orgId);
        setRlsContext(orgId);
        var report = getReportEntity(id, orgId);
        var sections = sectionRepository.findByReportIdAndOrgIdOrderByDisplayOrderAsc(id, orgId);
        return toResponse(report, sections);
    }

    @Transactional
    public DrilldownReportResponse updateReport(UUID id, UUID orgId, UUID userId, DrilldownReportRequest request) {
        requireOrg(orgId);
        setRlsContext(orgId);
        var entity = getReportEntity(id, orgId);
        applyRequest(entity, orgId, userId, request);
        var saved = definitionRepository.save(entity);

        sectionRepository.deleteByReportIdAndOrgId(id, orgId);
        var sections = saveSections(id, orgId, request.sections());
        log.info("Updated drilldown report id={} org={} sections={}", id, orgId, sections.size());
        return toResponse(saved, sections);
    }

    @Transactional
    public void deleteReport(UUID id, UUID orgId) {
        requireOrg(orgId);
        setRlsContext(orgId);
        var entity = getReportEntity(id, orgId);
        sectionRepository.deleteByReportIdAndOrgId(id, orgId);
        definitionRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public DrilldownReportQueryResponse queryReport(UUID id, UUID orgId, DrilldownReportQueryRequest request) {
        requireOrg(orgId);
        setRlsContext(orgId);
        getReportEntity(id, orgId);

        var filters = request != null && request.filters() != null ? request.filters() : Map.<String, Object>of();
        var sections = sectionRepository.findByReportIdAndOrgIdOrderByDisplayOrderAsc(id, orgId);
        var results = new LinkedHashMap<String, Object>();
        for (var section : sections) {
            results.put(section.getSectionKey(), executeSectionQuery(orgId, section, filters));
        }
        return new DrilldownReportQueryResponse(id, filters, results);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> drill(UUID id, UUID orgId, DrilldownReportDrillRequest request) {
        requireOrg(orgId);
        setRlsContext(orgId);
        getReportEntity(id, orgId);

        var section = sectionRepository.findByReportIdAndOrgIdOrderByDisplayOrderAsc(id, orgId).stream()
                .filter(s -> s.getSectionKey().equals(request.sectionKey()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Drilldown section not found: " + request.sectionKey()));

        var drillConfig = deserializeMap(section.getDrillConfig());
        var queryConfig = deserializeMap(section.getQueryConfig());
        String sql = stringValue(drillConfig.get("sql"));
        if (sql == null) {
            sql = stringValue(queryConfig.get("detail_sql"));
        }
        if (sql == null) {
            sql = stringValue(queryConfig.get("sql"));
        }

        var page = Math.max(request.page() != null ? request.page() : 0, 0);
        var size = Math.min(Math.max(request.size() != null ? request.size() : DEFAULT_DRILL_SIZE, 1), MAX_DRILL_SIZE);
        var response = sql != null
                ? aggregationService.executeRawSql(orgId, appendPaging(sql, size, page * size))
                : rawTableFallback(orgId, size);

        var result = new LinkedHashMap<String, Object>();
        result.put("report_id", id);
        result.put("section_key", section.getSectionKey());
        result.put("filters", request.filters() != null ? request.filters() : Map.of());
        result.put("selected_value", request.selectedValue());
        result.put("page", page);
        result.put("size", size);
        result.put("columns", response.columns());
        result.put("rows", response.rows());
        result.put("total_rows", response.totalRows());
        return result;
    }

    @Transactional
    public DrilldownReportViewStateResponse saveViewState(
            UUID id,
            UUID orgId,
            UUID userId,
            DrilldownReportViewStateRequest request) {
        requireOrg(orgId);
        setRlsContext(orgId);
        getReportEntity(id, orgId);

        var entity = new DrilldownReportViewEntity();
        entity.setReportId(id);
        entity.setOrgId(orgId);
        entity.setUserId(userId != null ? userId : SYSTEM_USER_ID);
        entity.setViewState(serialize(request != null ? request.viewState() : Map.of()));
        entity.setExpiresAt(request != null ? request.expiresAt() : null);
        var saved = viewRepository.save(entity);
        return new DrilldownReportViewStateResponse(
                saved.getId(),
                saved.getReportId(),
                deserialize(saved.getViewState()),
                saved.getCreatedAt(),
                saved.getExpiresAt()
        );
    }

    private DrilldownReportDefinitionEntity getReportEntity(UUID id, UUID orgId) {
        return definitionRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Drilldown report not found: " + id));
    }

    private void applyRequest(
            DrilldownReportDefinitionEntity entity,
            UUID orgId,
            UUID userId,
            DrilldownReportRequest request) {
        entity.setOrgId(orgId);
        entity.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy()
                : (userId != null ? userId : SYSTEM_USER_ID));
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setReportType(request.reportType() != null ? request.reportType() : "ANALYTICAL");
        entity.setBasePeriodType(request.basePeriodType());
        entity.setDefaultFilters(serialize(request.defaultFilters()));
        entity.setLayoutConfig(serialize(request.layoutConfig()));
        entity.setPublic(request.isPublic());
    }

    private List<DrilldownReportSectionEntity> saveSections(
            UUID reportId,
            UUID orgId,
            List<DrilldownReportSectionRequest> sectionRequests) {
        if (sectionRequests == null || sectionRequests.isEmpty()) {
            return List.of();
        }

        var entities = new java.util.ArrayList<DrilldownReportSectionEntity>();
        for (int i = 0; i < sectionRequests.size(); i++) {
            var request = sectionRequests.get(i);
            var entity = new DrilldownReportSectionEntity();
            entity.setReportId(reportId);
            entity.setOrgId(orgId);
            entity.setSectionKey(request.sectionKey());
            entity.setTitle(request.title());
            entity.setComponentType(request.componentType() != null ? request.componentType() : "TABLE");
            entity.setSourceType(request.sourceType() != null ? request.sourceType() : "AGGREGATION");
            entity.setSourceRefId(request.sourceRefId());
            entity.setQueryConfig(serialize(request.queryConfig()));
            entity.setDrillConfig(serialize(request.drillConfig()));
            entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : i);
            entities.add(entity);
        }
        return sectionRepository.saveAll(entities);
    }

    private Object executeSectionQuery(UUID orgId, DrilldownReportSectionEntity section, Map<String, Object> runtimeFilters) {
        var queryConfig = deserializeMap(section.getQueryConfig());
        var sql = stringValue(queryConfig.get("sql"));
        if (sql != null) {
            var raw = aggregationService.executeRawSql(orgId, sql);
            var result = new LinkedHashMap<String, Object>();
            result.put("type", "raw_sql");
            result.put("columns", raw.columns());
            result.put("rows", raw.rows());
            result.put("total_rows", raw.totalRows());
            return result;
        }

        var request = new DashboardDataRequest(
                stringList(queryConfig.get("group_by"), queryConfig.get("groupBy")),
                stringValue(queryConfig.getOrDefault("aggregation", "SUM")),
                stringValue(queryConfig.get("value_field"), queryConfig.get("valueField")),
                mergeFilters(queryConfig.get("filters"), runtimeFilters),
                stringValue(queryConfig.get("date_from"), queryConfig.get("dateFrom")),
                stringValue(queryConfig.get("date_to"), queryConfig.get("dateTo")),
                stringValue(queryConfig.getOrDefault("source_type", "ALL"))
        );
        var data = aggregationService.executeQuery(orgId, request);
        var result = new LinkedHashMap<String, Object>();
        result.put("type", "aggregation");
        result.put("rows", data.data());
        result.put("metadata", data.metadata());
        result.put("total_rows", data.data().size());
        return result;
    }

    private com.reportplatform.dash.model.dto.RawSqlResponse rawTableFallback(UUID orgId, int size) {
        var rows = aggregationService.getTableSummaries(orgId).stream()
                .limit(size)
                .map(row -> List.<Object>of(
                        row.get("id"),
                        row.get("file_id"),
                        row.get("source_sheet"),
                        row.get("row_count")))
                .toList();
        return new com.reportplatform.dash.model.dto.RawSqlResponse(
                List.of("id", "file_id", "source_sheet", "row_count"),
                rows,
                rows.size()
        );
    }

    private DrilldownReportResponse toResponse(
            DrilldownReportDefinitionEntity entity,
            List<DrilldownReportSectionEntity> sections) {
        return new DrilldownReportResponse(
                entity.getId(),
                entity.getOrgId(),
                entity.getName(),
                entity.getDescription(),
                entity.getReportType(),
                entity.getBasePeriodType(),
                deserialize(entity.getDefaultFilters()),
                deserialize(entity.getLayoutConfig()),
                entity.isPublic(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                sections.stream().map(this::toSectionResponse).toList()
        );
    }

    private DrilldownReportSectionResponse toSectionResponse(DrilldownReportSectionEntity entity) {
        return new DrilldownReportSectionResponse(
                entity.getId(),
                entity.getSectionKey(),
                entity.getTitle(),
                entity.getComponentType(),
                entity.getSourceType(),
                entity.getSourceRefId(),
                deserialize(entity.getQueryConfig()),
                deserialize(entity.getDrillConfig()),
                entity.getDisplayOrder()
        );
    }

    private String serialize(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON config: " + e.getMessage());
        }
    }

    private Object deserialize(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize drilldown JSON", e);
            return Map.of();
        }
    }

    private Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private List<String> stringList(Object first, Object fallback) {
        Object value = first != null ? first : fallback;
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof String string && !string.isBlank()) {
            return List.of(string);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query_config.group_by is required");
    }

    private Map<String, String> mergeFilters(Object baseFilters, Map<String, Object> runtimeFilters) {
        var merged = new LinkedHashMap<String, String>();
        if (baseFilters instanceof Map<?, ?> map) {
            map.forEach((key, value) -> merged.put(String.valueOf(key), String.valueOf(value)));
        }
        runtimeFilters.forEach((key, value) -> {
            if (value != null) {
                merged.put(key, String.valueOf(value));
            }
        });
        return merged;
    }

    private String stringValue(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private String stringValue(Object first, Object fallback) {
        var value = first != null ? first : fallback;
        return stringValue(value);
    }

    private String appendPaging(String sql, int limit, int offset) {
        var normalized = sql.trim().replaceAll(";+$", "");
        var upper = normalized.toUpperCase();
        if (upper.contains(" LIMIT ")) {
            return normalized;
        }
        return normalized + " LIMIT " + limit + " OFFSET " + offset;
    }

    private void requireOrg(UUID orgId) {
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "X-Org-Id header is required and must be a valid UUID");
        }
    }
}
