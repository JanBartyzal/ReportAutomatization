package com.reportplatform.dash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.dash.model.DashboardEntity;
import com.reportplatform.dash.model.dto.DashboardListResponse;
import com.reportplatform.dash.model.dto.DashboardRequest;
import com.reportplatform.dash.model.dto.DashboardResponse;
import com.reportplatform.dash.repository.DashboardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final DashboardRepository dashboardRepository;
    private final ObjectMapper objectMapper;

    public DashboardService(DashboardRepository dashboardRepository, ObjectMapper objectMapper) {
        this.dashboardRepository = dashboardRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DashboardListResponse listDashboards(UUID orgId, UUID userId) {
        if (orgId == null) {
            return new DashboardListResponse(java.util.List.of());
        }
        var entities = dashboardRepository.findAccessibleDashboards(orgId, userId);
        var responses = entities.stream()
                .map(this::toResponse)
                .toList();
        return new DashboardListResponse(responses);
    }

    @Transactional
    public DashboardResponse createDashboard(UUID orgId, UUID userId, DashboardRequest request) {
        var entity = new DashboardEntity();
        entity.setOrgId(orgId);
        entity.setCreatedBy(userId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setConfig(serializeConfig(request.config()));
        entity.setChartType(request.chartType() != null ? request.chartType() : "bar");
        entity.setPublic(request.isPublic());

        var saved = dashboardRepository.save(entity);
        log.info("Created dashboard id={} org={} user={}", saved.getId(), orgId, userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID id, UUID orgId) {
        var entity = dashboardRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dashboard not found: " + id));
        return toResponse(entity);
    }

    @Transactional
    public DashboardResponse updateDashboard(UUID id, UUID orgId, UUID userId, DashboardRequest request) {
        var entity = dashboardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dashboard not found: " + id));

        // Merge: only update non-null fields
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.config() != null) entity.setConfig(serializeConfig(request.config()));
        if (request.chartType() != null) entity.setChartType(request.chartType());
        // isPublic is a primitive boolean — always update
        entity.setPublic(request.isPublic());

        var saved = dashboardRepository.save(entity);
        log.info("Updated dashboard id={} org={} user={}", id, orgId, userId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteDashboard(UUID id, UUID orgId) {
        var entity = dashboardRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dashboard not found: " + id));
        dashboardRepository.delete(entity);
        log.info("Deleted dashboard id={} org={}", id, orgId);
    }

    private DashboardResponse toResponse(DashboardEntity entity) {
        return new DashboardResponse(
                entity.getId(),
                entity.getOrgId(),
                entity.getCreatedBy(),
                entity.getName(),
                entity.getDescription(),
                deserializeConfig(entity.getConfig()),
                entity.getChartType(),
                entity.isPublic(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String serializeConfig(Object config) {
        if (config == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid config JSON: " + e.getMessage());
        }
    }

    private Object deserializeConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize config JSON, returning raw string", e);
            return configJson;
        }
    }
}
