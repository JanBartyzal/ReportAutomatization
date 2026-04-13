package com.reportplatform.excelsync.service;

import com.reportplatform.excelsync.connector.FileConnector;
import com.reportplatform.excelsync.model.entity.*;
import com.reportplatform.excelsync.repository.ExportFlowDefinitionRepository;
import com.reportplatform.excelsync.repository.ExportFlowExecutionRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportFlowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExportFlowExecutionService.class);

    private final ExportFlowDefinitionRepository definitionRepository;
    private final ExportFlowExecutionRepository executionRepository;
    private final FileConnectorFactory fileConnectorFactory;
    private final ConcurrencyGuard concurrencyGuard;
    private final DaprClient daprClient;

    public ExportFlowExecutionService(ExportFlowDefinitionRepository definitionRepository,
                                      ExportFlowExecutionRepository executionRepository,
                                      FileConnectorFactory fileConnectorFactory,
                                      ConcurrencyGuard concurrencyGuard,
                                      DaprClient daprClient) {
        this.definitionRepository = definitionRepository;
        this.executionRepository = executionRepository;
        this.fileConnectorFactory = fileConnectorFactory;
        this.concurrencyGuard = concurrencyGuard;
        this.daprClient = daprClient;
    }

    @Async("excelSyncExecutor")
    public void executeFlow(UUID flowId, UUID orgId, String triggerSource, String triggerEventId) {
        ExportFlowDefinitionEntity flow = definitionRepository.findByIdAndOrgId(flowId, orgId)
                .orElse(null);
        if (flow == null) {
            log.error("Export flow [{}] not found for org [{}]", flowId, orgId);
            return;
        }

        // Acquire concurrency lock
        String lockValue = concurrencyGuard.tryAcquire(flowId);
        if (lockValue == null) {
            log.warn("Skipping execution of flow [{}] - concurrent execution in progress", flowId);
            return;
        }

        // Create execution record
        ExportFlowExecutionEntity execution = new ExportFlowExecutionEntity();
        execution.setFlowId(flowId);
        execution.setOrgId(orgId);
        execution.setTriggerSource(triggerSource);
        execution.setTriggerEventId(triggerEventId);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        execution = executionRepository.save(execution);

        try {
            log.info("Starting execution [{}] for flow [{}]", execution.getId(), flowId);

            // Step 1: Execute SQL query via engine-data (Dapr service invocation)
            Map<String, String> internalHeaders = Map.of(
                    "X-User-Id", "system-excel-sync",
                    "X-Roles",   "ADMIN",
                    "X-Org-Id",  orgId.toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> sqlResult = daprClient.invokeMethod(
                    "engine-data", "/api/dashboards/sql/execute",
                    Map.of("sql", flow.getSqlQuery()),
                    HttpExtension.POST, internalHeaders, Map.class)
                    .block(Duration.ofSeconds(60));

            if (sqlResult == null) {
                throw new RuntimeException("Null response from engine-data SQL execution");
            }

            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) sqlResult.getOrDefault("columns", List.of());
            @SuppressWarnings("unchecked")
            List<List<Object>> rows = (List<List<Object>>) sqlResult.getOrDefault("rows", List.of());
            int rowCount = ((Number) sqlResult.getOrDefault("totalRows", rows.size())).intValue();
            log.info("SQL query executed, fetched {} rows", rowCount);

            // Step 2: Resolve target file name
            String fileName = resolveFileName(flow);
            execution.setTargetPathUsed(flow.getTargetPath() + "/" + fileName);

            // Step 3: Fetch existing Excel from target
            FileConnector connector = fileConnectorFactory.getConnector(flow.getTargetType());
            byte[] existingExcel = connector.download(flow, fileName);

            // Step 4: Call processor-generators UpdateSheet via Dapr HTTP
            String excelBase64 = existingExcel != null
                    ? Base64.getEncoder().encodeToString(existingExcel)
                    : "";
            List<List<Map<String, Object>>> dataRows = rows.stream()
                    .map(row -> row.stream().map(this::toCellValue).toList())
                    .toList();
            Map<String, Object> updateSheetReq = new HashMap<>();
            updateSheetReq.put("excel_base64", excelBase64);
            updateSheetReq.put("sheet_name", flow.getTargetSheet());
            updateSheetReq.put("headers", columns);
            updateSheetReq.put("data_rows", dataRows);
            updateSheetReq.put("auto_filter", true);
            updateSheetReq.put("freeze_header", true);
            updateSheetReq.put("auto_column_width", true);

            @SuppressWarnings("unchecked")
            Map<String, Object> updateResponse = daprClient.invokeMethod(
                    "processor-generators", "/api/v1/excel/update-sheet",
                    updateSheetReq,
                    HttpExtension.POST, Map.class)
                    .block(Duration.ofSeconds(120));

            if (updateResponse == null) {
                throw new RuntimeException("Null response from processor-generators UpdateSheet");
            }

            String updatedBase64 = (String) updateResponse.get("excel_base64");
            byte[] updatedExcel = (updatedBase64 != null && !updatedBase64.isEmpty())
                    ? Base64.getDecoder().decode(updatedBase64)
                    : (existingExcel != null ? existingExcel : new byte[0]);
            log.info("Sheet [{}] updated, rows written: {}",
                    flow.getTargetSheet(), updateResponse.getOrDefault("rows_written", rowCount));

            // Step 5: Write updated Excel back to target
            if (updatedExcel.length > 0) {
                connector.upload(flow, updatedExcel, fileName);
            }

            // Step 6: Mark execution as successful
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setRowsExported(rowCount);
            execution.setCompletedAt(Instant.now());
            executionRepository.save(execution);

            log.info("Execution [{}] completed successfully for flow [{}]",
                    execution.getId(), flowId);

            // Step 7: Publish notification
            publishNotification(flow, execution, null);

        } catch (Exception e) {
            log.error("Execution [{}] failed for flow [{}]: {}",
                    execution.getId(), flowId, e.getMessage(), e);

            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(Instant.now());
            executionRepository.save(execution);

            publishNotification(flow, execution, e.getMessage());
        } finally {
            concurrencyGuard.release(flowId, lockValue);
        }
    }

    private String resolveFileName(ExportFlowDefinitionEntity flow) {
        if (flow.getFileNaming() == FileNaming.CUSTOM && flow.getCustomFileName() != null) {
            String name = flow.getCustomFileName();
            if (!name.endsWith(".xlsx")) {
                name += ".xlsx";
            }
            return name;
        }
        return flow.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".xlsx";
    }

    private Map<String, Object> toCellValue(Object value) {
        if (value == null) {
            return Map.of("type", "string", "value", "");
        }
        if (value instanceof Number) {
            return Map.of("type", "number", "value", value);
        }
        if (value instanceof Boolean) {
            return Map.of("type", "bool", "value", value);
        }
        if (value instanceof java.util.Date || value instanceof java.time.temporal.Temporal) {
            return Map.of("type", "date", "value", value.toString());
        }
        return Map.of("type", "string", "value", value.toString());
    }

    private void publishNotification(ExportFlowDefinitionEntity flow,
                                     ExportFlowExecutionEntity execution,
                                     String error) {
        try {
            String type = execution.getStatus() == ExecutionStatus.SUCCESS
                    ? "EXPORT_COMPLETED" : "EXPORT_FAILED";

            var event = Map.of(
                    "type", type,
                    "flowName", flow.getName(),
                    "flowId", flow.getId().toString(),
                    "executionId", execution.getId().toString(),
                    "targetPath", execution.getTargetPathUsed() != null ? execution.getTargetPathUsed() : "",
                    "error", error != null ? error : "",
                    "timestamp", Instant.now().toString());

            daprClient.publishEvent("reportplatform-pubsub", "notify", event).block();
            log.info("Published {} notification for flow [{}]", type, flow.getId());
        } catch (Exception e) {
            log.error("Failed to publish notification for flow [{}]: {}",
                    flow.getId(), e.getMessage());
        }
    }
}
