package com.reportplatform.excelsync.service;

import com.reportplatform.excelsync.model.dto.*;
import com.reportplatform.excelsync.model.entity.*;
import com.reportplatform.excelsync.repository.ExportFlowDefinitionRepository;
import com.reportplatform.excelsync.repository.ExportFlowExecutionRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ExportFlowService {

    private static final Logger log = LoggerFactory.getLogger(ExportFlowService.class);

    private static final Pattern INVALID_SHEET_CHARS = Pattern.compile("[\\[\\]:*?/\\\\]");
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    private final ExportFlowDefinitionRepository definitionRepository;
    private final ExportFlowExecutionRepository executionRepository;
    private final DaprClient daprClient;

    public ExportFlowService(ExportFlowDefinitionRepository definitionRepository,
                             ExportFlowExecutionRepository executionRepository,
                             DaprClient daprClient) {
        this.definitionRepository = definitionRepository;
        this.executionRepository = executionRepository;
        this.daprClient = daprClient;
    }

    @Transactional(readOnly = true)
    public List<ExportFlowDTO> listFlows(UUID orgId) {
        return definitionRepository.findByOrgIdAndIsActiveTrue(orgId).stream()
                .map(ExportFlowDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportFlowDTO getFlow(UUID id, UUID orgId) {
        ExportFlowDefinitionEntity entity = definitionRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Export Flow not found: " + id));
        ExportFlowDTO dto = ExportFlowDTO.fromEntity(entity);
        executionRepository.findFirstByFlowIdOrderByStartedAtDesc(id)
                .ifPresent(exec -> dto.setLastExecution(ExportFlowExecutionDTO.fromEntity(exec)));
        return dto;
    }

    @Transactional
    public ExportFlowDTO createFlow(UUID orgId, String userId, CreateExportFlowRequest request) {
        validateSheetName(request.getTargetSheet());
        validateSqlQuery(request.getSqlQuery());
        validateTargetPath(request.getTargetType(), request.getTargetPath());

        ExportFlowDefinitionEntity entity = new ExportFlowDefinitionEntity();
        entity.setOrgId(orgId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSqlQuery(request.getSqlQuery());
        entity.setTargetType(request.getTargetType());
        entity.setTargetPath(request.getTargetPath());
        entity.setTargetSheet(request.getTargetSheet());
        entity.setFileNaming(request.getFileNaming() != null ? request.getFileNaming() : FileNaming.CUSTOM);
        entity.setCustomFileName(request.getCustomFileName());
        entity.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : TriggerType.MANUAL);
        entity.setTriggerFilter(request.getTriggerFilter());
        entity.setSharepointConfig(request.getSharepointConfig());
        entity.setCreatedBy(userId);

        entity = definitionRepository.save(entity);
        log.info("Created Export Flow [{}] for org [{}] by user [{}]", entity.getId(), orgId, userId);
        return ExportFlowDTO.fromEntity(entity);
    }

    @Transactional
    public ExportFlowDTO updateFlow(UUID id, UUID orgId, UpdateExportFlowRequest request) {
        ExportFlowDefinitionEntity entity = definitionRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Export Flow not found: " + id));

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getSqlQuery() != null) {
            validateSqlQuery(request.getSqlQuery());
            entity.setSqlQuery(request.getSqlQuery());
        }
        if (request.getTargetType() != null) entity.setTargetType(request.getTargetType());
        if (request.getTargetPath() != null) {
            validateTargetPath(
                    request.getTargetType() != null ? request.getTargetType() : entity.getTargetType(),
                    request.getTargetPath());
            entity.setTargetPath(request.getTargetPath());
        }
        if (request.getTargetSheet() != null) {
            validateSheetName(request.getTargetSheet());
            entity.setTargetSheet(request.getTargetSheet());
        }
        if (request.getFileNaming() != null) entity.setFileNaming(request.getFileNaming());
        if (request.getCustomFileName() != null) entity.setCustomFileName(request.getCustomFileName());
        if (request.getTriggerType() != null) entity.setTriggerType(request.getTriggerType());
        if (request.getTriggerFilter() != null) entity.setTriggerFilter(request.getTriggerFilter());
        if (request.getSharepointConfig() != null) entity.setSharepointConfig(request.getSharepointConfig());

        entity = definitionRepository.save(entity);
        log.info("Updated Export Flow [{}]", id);
        return ExportFlowDTO.fromEntity(entity);
    }

    @Transactional
    public void softDeleteFlow(UUID id, UUID orgId) {
        ExportFlowDefinitionEntity entity = definitionRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Export Flow not found: " + id));
        entity.setActive(false);
        definitionRepository.save(entity);
        log.info("Soft-deleted Export Flow [{}]", id);
    }

    @Transactional(readOnly = true)
    public ExportFlowTestResult testFlow(UUID id, UUID orgId) {
        ExportFlowDefinitionEntity entity = definitionRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Export Flow not found: " + id));

        try {
            validateSqlQuery(entity.getSqlQuery());

            Map<String, String> headers = Map.of(
                    "X-User-Id", "system-excel-sync",
                    "X-Roles",   "ADMIN",
                    "X-Org-Id",  orgId.toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> sqlResult = daprClient.invokeMethod(
                    "engine-data", "/api/dashboards/sql/execute",
                    Map.of("sql", entity.getSqlQuery()),
                    HttpExtension.POST, headers, Map.class)
                    .block(Duration.ofSeconds(30));

            if (sqlResult == null) {
                return ExportFlowTestResult.error("No response from SQL execution engine");
            }

            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) sqlResult.getOrDefault("columns", List.of());
            @SuppressWarnings("unchecked")
            List<List<Object>> rows = (List<List<Object>>) sqlResult.getOrDefault("rows", List.of());
            int totalRows = ((Number) sqlResult.getOrDefault("totalRows", rows.size())).intValue();

            int previewLimit = Math.min(5, rows.size());
            boolean truncated = totalRows > previewLimit;

            List<Map<String, Object>> previewRows = rows.subList(0, previewLimit).stream()
                    .map(row -> {
                        Map<String, Object> rowMap = new LinkedHashMap<>();
                        for (int i = 0; i < columns.size() && i < row.size(); i++) {
                            rowMap.put(columns.get(i), row.get(i));
                        }
                        return (Map<String, Object>) rowMap;
                    })
                    .toList();

            return new ExportFlowTestResult(columns, previewRows, totalRows, truncated);

        } catch (IllegalArgumentException e) {
            return ExportFlowTestResult.error(e.getMessage());
        } catch (Exception e) {
            log.warn("Test flow [{}] SQL execution failed: {}", id, e.getMessage());
            return ExportFlowTestResult.error("SQL execution failed: " + e.getMessage());
        }
    }

    private void validateSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            throw new IllegalArgumentException("Sheet name cannot be empty");
        }
        if (sheetName.length() > MAX_SHEET_NAME_LENGTH) {
            throw new IllegalArgumentException("Sheet name exceeds maximum length of " + MAX_SHEET_NAME_LENGTH);
        }
        if (INVALID_SHEET_CHARS.matcher(sheetName).find()) {
            throw new IllegalArgumentException("Sheet name contains invalid characters: []:*?/\\");
        }
    }

    private void validateSqlQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }
        String upper = sqlQuery.trim().toUpperCase();
        if (upper.startsWith("INSERT") || upper.startsWith("UPDATE") || upper.startsWith("DELETE")
                || upper.startsWith("DROP") || upper.startsWith("ALTER") || upper.startsWith("CREATE")
                || upper.startsWith("TRUNCATE")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
    }

    private void validateTargetPath(TargetType targetType, String targetPath) {
        if (targetPath == null || targetPath.isBlank()) {
            throw new IllegalArgumentException("Target path cannot be empty");
        }
        if (targetType == TargetType.SHAREPOINT) {
            if (!targetPath.startsWith("https://") || !targetPath.contains("sharepoint.com")) {
                throw new IllegalArgumentException("Invalid SharePoint URL format");
            }
        }
    }
}
