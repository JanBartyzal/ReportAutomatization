package com.reportplatform.dash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.dash.model.ComparisonKpiEntity;
import com.reportplatform.dash.model.dto.*;
import com.reportplatform.dash.model.dto.MultiOrgComparisonResponse.OrgComparisonRow;
import com.reportplatform.dash.repository.ComparisonKpiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ComparisonService.class);

    private final ComparisonKpiRepository kpiRepository;
    private final AggregationService aggregationService;
    private final ObjectMapper objectMapper;

    public ComparisonService(ComparisonKpiRepository kpiRepository,
                              AggregationService aggregationService,
                              ObjectMapper objectMapper) {
        this.kpiRepository = kpiRepository;
        this.aggregationService = aggregationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ComparisonKpiResponse createKpi(UUID orgId, UUID userId, ComparisonKpiRequest request) {
        var entity = new ComparisonKpiEntity();
        entity.setOrgId(orgId);
        entity.setCreatedBy(userId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setValueField(request.valueField());
        entity.setAggregation(request.aggregation() != null ? request.aggregation() : "SUM");
        entity.setGroupBy(serializeList(request.groupBy()));
        entity.setSourceType(request.sourceType() != null ? request.sourceType() : "ALL");
        entity.setNormalization(request.normalization() != null ? request.normalization() : "NONE");

        entity = kpiRepository.save(entity);
        log.info("Created comparison KPI: id={} name={}", entity.getId(), entity.getName());
        return toKpiResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ComparisonKpiResponse> listKpis(UUID orgId) {
        return kpiRepository.findByOrgIdAndActiveTrueOrderByNameAsc(orgId).stream()
                .map(this::toKpiResponse)
                .toList();
    }

    @Transactional
    public void deactivateKpi(UUID kpiId) {
        var kpi = kpiRepository.findById(kpiId)
                .orElseThrow(() -> new IllegalArgumentException("KPI not found: " + kpiId));
        kpi.setActive(false);
        kpiRepository.save(kpi);
    }

    public MultiOrgComparisonResponse compareAcrossOrgs(MultiOrgComparisonRequest request) {
        List<OrgComparisonRow> rows = new ArrayList<>();

        for (String orgId : request.orgIds()) {
            UUID orgUuid = UUID.fromString(orgId);

            var dataRequest = new DashboardDataRequest(
                    request.groupBy(), request.aggregation(), request.valueField(),
                    null, request.dateFrom(), request.dateTo(), request.sourceType()
            );

            var result = aggregationService.executeQuery(orgUuid, dataRequest);

            for (var row : result.data()) {
                Map<String, Object> groupKey = new LinkedHashMap<>();
                for (String field : request.groupBy()) {
                    String alias = field.replaceAll("[^a-zA-Z0-9_]", "_");
                    groupKey.put(field, row.getOrDefault(alias, null));
                }

                double value = extractDouble(row.get("agg_value"));
                Double normalizedValue = normalizeValue(value, request.normalization(),
                        request.dateFrom(), request.dateTo());

                rows.add(new OrgComparisonRow(orgId, groupKey, value, normalizedValue));
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("orgCount", request.orgIds().size());
        metadata.put("aggregation", request.aggregation());
        metadata.put("valueField", request.valueField());
        metadata.put("normalization", request.normalization());

        return new MultiOrgComparisonResponse(rows, metadata);
    }

    private Double normalizeValue(double value, String normalization, String dateFrom, String dateTo) {
        if (normalization == null || "NONE".equals(normalization)) {
            return null;
        }
        // Normalization requires date range to calculate days
        if (dateFrom == null || dateTo == null) {
            return null;
        }
        try {
            var from = java.time.LocalDate.parse(dateFrom);
            var to = java.time.LocalDate.parse(dateTo);
            long days = java.time.temporal.ChronoUnit.DAYS.between(from, to);
            if (days <= 0) return null;

            return switch (normalization) {
                case "DAILY" -> value / days;
                case "MONTHLY" -> (value / days) * 30.0;
                case "ANNUAL" -> (value / days) * 365.0;
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Failed to normalize value: {}", e.getMessage());
            return null;
        }
    }

    private double extractDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number num) return num.doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) {
            log.warn("Cannot parse numeric value '{}' as double, treating as 0.0", val);
            return 0.0;
        }
    }

    private ComparisonKpiResponse toKpiResponse(ComparisonKpiEntity entity) {
        return new ComparisonKpiResponse(
                entity.getId(), entity.getName(), entity.getDescription(),
                entity.getValueField(), entity.getAggregation(),
                deserializeList(entity.getGroupBy()),
                entity.getSourceType(), entity.getNormalization(),
                entity.isActive(), entity.getCreatedAt()
        );
    }

    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
