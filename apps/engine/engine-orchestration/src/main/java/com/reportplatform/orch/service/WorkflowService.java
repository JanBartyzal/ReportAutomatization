package com.reportplatform.orch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.orch.config.ServiceRoutingConfig;
import com.reportplatform.orch.config.WorkflowEvent;
import com.reportplatform.orch.config.WorkflowState;
import com.reportplatform.orch.exception.ParsingException;
import com.reportplatform.orch.exception.StorageException;
import com.reportplatform.orch.model.FailedJobEntity;
import com.reportplatform.orch.model.WorkflowHistoryEntity;
import com.reportplatform.orch.repository.FailedJobRepository;
import com.reportplatform.orch.repository.WorkflowHistoryRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core workflow orchestration service.
 * <p>
 * Coordinates the file processing pipeline by managing state machine
 * transitions,
 * invoking downstream microservices via Dapr, and handling failures through the
 * saga pattern.
 * </p>
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private static final String FILE_ID_HEADER = "fileId";
    private static final String WORKFLOW_ID_HEADER = "workflowId";
    private static final String ERROR_DETAIL_HEADER = "errorDetail";

    private final StateMachineFactory<WorkflowState, WorkflowEvent> stateMachineFactory;
    private final SagaOrchestrator sagaOrchestrator;
    private final FileTypeRouter fileTypeRouter;
    private final IdempotencyService idempotencyService;
    private final DaprClient daprClient;
    private final DirectServiceClient directServiceClient;
    private final WorkflowHistoryRepository workflowHistoryRepository;
    private final FailedJobRepository failedJobRepository;
    private final ObjectMapper objectMapper;
    private final ServiceRoutingConfig routingConfig;

    public WorkflowService(StateMachineFactory<WorkflowState, WorkflowEvent> stateMachineFactory,
            SagaOrchestrator sagaOrchestrator,
            FileTypeRouter fileTypeRouter,
            IdempotencyService idempotencyService,
            DaprClient daprClient,
            DirectServiceClient directServiceClient,
            WorkflowHistoryRepository workflowHistoryRepository,
            FailedJobRepository failedJobRepository,
            ObjectMapper objectMapper,
            ServiceRoutingConfig routingConfig) {
        this.stateMachineFactory = stateMachineFactory;
        this.sagaOrchestrator = sagaOrchestrator;
        this.fileTypeRouter = fileTypeRouter;
        this.idempotencyService = idempotencyService;
        this.daprClient = daprClient;
        this.directServiceClient = directServiceClient;
        this.workflowHistoryRepository = workflowHistoryRepository;
        this.failedJobRepository = failedJobRepository;
        this.objectMapper = objectMapper;
        this.routingConfig = routingConfig;
    }

    /**
     * Starts a new file processing workflow.
     *
     * @param fileId   unique file identifier
     * @param fileType file extension (e.g. "PPTX")
     * @param orgId    organization identifier
     * @return the generated workflow ID
     */
    public String startWorkflow(String fileId, String fileType, String orgId) {
        String workflowId = UUID.randomUUID().toString();
        log.info("Starting workflow [{}] for file [{}] type [{}] org [{}]",
                workflowId, fileId, fileType, orgId);

        // Persist workflow history
        var history = new WorkflowHistoryEntity(fileId, workflowId,
                WorkflowState.RECEIVED.name(), orgId);
        workflowHistoryRepository.save(history);

        // Build and start the state machine
        StateMachine<WorkflowState, WorkflowEvent> sm = stateMachineFactory.getStateMachine(workflowId);
        sm.startReactively().block();

        // Trigger FILE_RECEIVED to move to SCANNING
        sendEvent(sm, WorkflowEvent.FILE_RECEIVED, fileId, workflowId);

        // Execute the saga pipeline
        var context = new SagaOrchestrator.SagaContext(workflowId, fileId, fileType, orgId);
        List<SagaOrchestrator.SagaStep> steps = buildPipelineSteps(sm, fileId, workflowId);
        SagaOrchestrator.SagaResult result = sagaOrchestrator.execute(steps, context);

        switch (result) {
            case SagaOrchestrator.SagaResult.Success success -> {
                history.markCompleted(WorkflowState.COMPLETED.name());
                history.setStepsJson(serializeSteps(steps.stream().map(SagaOrchestrator.SagaStep::name).toList()));
                workflowHistoryRepository.save(history);
                log.info("Workflow [{}] completed successfully", workflowId);
            }
            case SagaOrchestrator.SagaResult.Failure failure -> {
                sendEvent(sm, WorkflowEvent.ERROR, fileId, workflowId);
                history.markCompleted(WorkflowState.FAILED.name());
                history.setStepsJson(serializeSteps(failure.compensatedSteps()));
                workflowHistoryRepository.save(history);

                // Record failed job
                var failedJob = new FailedJobEntity(
                        fileId, workflowId,
                        failure.cause().getClass().getSimpleName(),
                        failure.cause().getMessage(),
                        orgId);
                failedJobRepository.save(failedJob);

                log.error("Workflow [{}] failed at step [{}]: {}",
                        workflowId, failure.failedStep(), failure.cause().getMessage());
            }
        }

        sm.stopReactively().block();
        return workflowId;
    }

    /**
     * Starts a FORM_IMPORT workflow for importing Excel data into forms.
     * This workflow parses the Excel file, suggests field mappings, and returns
     * mapping suggestions to the frontend for user confirmation.
     *
     * @param fileId   unique file identifier
     * @param fileType file extension (e.g., "XLSX")
     * @param orgId    organization identifier
     * @param blobUrl  the blob URL of the uploaded file
     * @return the generated workflow ID
     */
    public String startFormImportWorkflow(String fileId, String fileType, String orgId, String blobUrl) {
        String workflowId = UUID.randomUUID().toString();
        log.info("Starting FORM_IMPORT workflow [{}] for file [{}] type [{}] org [{}]",
                workflowId, fileId, fileType, orgId);

        // Persist workflow history with FORM_IMPORT type
        var history = new WorkflowHistoryEntity(fileId, workflowId,
                "FORM_IMPORT", orgId);
        workflowHistoryRepository.save(history);

        try {
            // Step 1: Parse Excel via processor-atomizers (gRPC call)
            log.info("FORM_IMPORT [{}]: Parsing Excel file", workflowId);
            var parseResult = parseExcelFile(fileId, blobUrl);

            // Step 2: Get suggested mapping from engine-data (template)
            log.info("FORM_IMPORT [{}]: Getting mapping suggestions", workflowId);
            var mappingResult = suggestMapping(orgId, parseResult.headers(), parseResult.sampleRows());

            // Step 3: Publish mapping suggestions event for frontend
            publishMappingSuggestionsEvent(workflowId, fileId, orgId, parseResult.headers(),
                    mappingResult.mappings(), mappingResult.unmappedColumns());

            // Mark workflow as awaiting user confirmation
            history.setStepsJson("['PARSE_EXCEL', 'SUGGEST_MAPPING', 'AWAITING_CONFIRMATION']");
            history.markCompleted("AWAITING_CONFIRMATION");
            workflowHistoryRepository.save(history);

            log.info("FORM_IMPORT workflow [{}] completed - awaiting user confirmation", workflowId);
            return workflowId;

        } catch (Exception e) {
            log.error("FORM_IMPORT workflow [{}] failed: {}", workflowId, e.getMessage(), e);
            history.markCompleted("FAILED");
            workflowHistoryRepository.save(history);
            throw new RuntimeException("FORM_IMPORT workflow failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses Excel file via processor-atomizers (Excel atomizer) gRPC call.
     */
    private ExcelParseResult parseExcelFile(String fileId, String blobUrl) {
        log.info("Calling processor-atomizers (Excel) to parse file: {}", fileId);

        try {
            // Build request for Excel atomizer
            Map<String, String> request = new java.util.HashMap<>();
            request.put("file_id", fileId);
            request.put("blob_url", blobUrl);

            // Call processor-atomizers (direct HTTP with Dapr fallback)
            Map<String, Object> response = directServiceClient.invokeMethod(
                    routingConfig.processorAtomizers(),
                    "/api/v1/excel/extract",
                    request,
                    Map.class);

            if (response != null) {
                // Extract headers and sample rows from response
                List<String> headers = new ArrayList<>();
                List<Object> sampleRows = new ArrayList<>();

                Object headersObj = response.get("headers");
                if (headersObj instanceof List) {
                    headers = new ArrayList<>((List<String>) headersObj);
                }

                Object rowsObj = response.get("rows");
                if (rowsObj instanceof List) {
                    List rowsList = (List) rowsObj;
                    // Take first 10 rows as samples
                    for (int i = 0; i < Math.min(10, rowsList.size()); i++) {
                        sampleRows.add(rowsList.get(i));
                    }
                }

                log.info("processor-atomizers (Excel) returned {} headers and {} sample rows", headers.size(), sampleRows.size());
                return new ExcelParseResult(headers, sampleRows);
            }
        } catch (Exception e) {
            log.error("Failed to call processor-atomizers (Excel): {}", e.getMessage(), e);
        }

        // Return empty result on failure
        return new ExcelParseResult(List.of(), List.of());
    }

    /**
     * Gets mapping suggestions from engine-data (template service).
     */
    private MappingResult suggestMapping(String orgId, List<String> headers,
            List<Object> sampleRows) {
        log.info("Calling engine-data (template) to suggest mappings for org: {}", orgId);

        try {
            // Build request for mapping suggestions
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("org_id", orgId);
            request.put("headers", headers);
            request.put("sample_rows", sampleRows);
            request.put("mapping_type", "EXCEL_TO_FORM");

            // Call engine-data (template service) - direct HTTP with Dapr fallback
            Map<String, Object> response = directServiceClient.invokeMethod(
                    routingConfig.engineData(),
                    "/api/v1/mapping/suggest",
                    request,
                    Map.class);

            if (response != null) {
                List<Object> mappings = new ArrayList<>();
                List<String> unmappedColumns = new ArrayList<>();

                Object mappingsObj = response.get("mappings");
                if (mappingsObj instanceof List) {
                    mappings = new ArrayList<>((List<Object>) mappingsObj);
                }

                Object unmappedObj = response.get("unmapped_columns");
                if (unmappedObj instanceof List) {
                    unmappedColumns = new ArrayList<>((List<String>) unmappedObj);
                }

                log.info("engine-data (template) returned {} mappings and {} unmapped columns", mappings.size(),
                        unmappedColumns.size());
                return new MappingResult(mappings, unmappedColumns);
            }
        } catch (Exception e) {
            log.error("Failed to call engine-data (template): {}", e.getMessage(), e);
        }

        // Return empty result on failure
        return new MappingResult(List.of(), headers);
    }

    /**
     * Publishes mapping suggestions event to frontend via Dapr Pub/Sub.
     */
    private void publishMappingSuggestionsEvent(String workflowId, String fileId, String orgId,
            java.util.List<String> excelHeaders, java.util.List<Object> mappings,
            java.util.List<String> unmappedColumns) {
        try {
            var event = new java.util.HashMap<String, Object>();
            event.put("workflowId", workflowId);
            event.put("fileId", fileId);
            event.put("orgId", orgId);
            event.put("excelHeaders", excelHeaders);
            event.put("mappings", mappings);
            event.put("unmappedColumns", unmappedColumns);
            event.put("timestamp", java.time.Instant.now().toString());

            daprClient.publishEvent(
                    "reportplatform-pubsub",
                    "form.import.mapping_suggestions",
                    event).block();
            log.info("Published mapping_suggestions event for workflow [{}]", workflowId);
        } catch (Exception e) {
            log.error("Failed to publish mapping_suggestions event: {}", e.getMessage(), e);
        }
    }

    private record ExcelParseResult(java.util.List<String> headers, java.util.List<Object> sampleRows) {
    }

    private record MappingResult(java.util.List<Object> mappings, java.util.List<String> unmappedColumns) {
    }

    /**
     * Retrieves the current status of a workflow.
     *
     * @param workflowId the workflow identifier
     * @return the workflow history entity, or null if not found
     */
    public WorkflowHistoryEntity getWorkflowStatus(String workflowId) {
        return workflowHistoryRepository.findByWorkflowId(workflowId).orElse(null);
    }

    /**
     * Retries a previously failed workflow by starting a fresh one for the same
     * file.
     *
     * @param workflowId the original failed workflow ID
     * @return the new workflow ID
     */
    public String retryWorkflow(String workflowId) {
        var history = workflowHistoryRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (!WorkflowState.FAILED.name().equals(history.getStatus())) {
            throw new IllegalStateException(
                    "Only failed workflows can be retried. Current status: " + history.getStatus());
        }

        log.info("Retrying workflow [{}] for file [{}]", workflowId, history.getFileId());
        // Start a new workflow for the same file - file type must be re-derived
        // For retry we store the file type in the context; fallback to UNKNOWN
        return startWorkflow(history.getFileId(), "UNKNOWN", history.getOrgId());
    }

    /**
     * Cancels a running workflow.
     *
     * @param workflowId the workflow identifier
     * @return true if cancellation succeeded
     */
    public boolean cancelWorkflow(String workflowId) {
        var history = workflowHistoryRepository.findByWorkflowId(workflowId).orElse(null);
        if (history == null) {
            return false;
        }
        if (WorkflowState.COMPLETED.name().equals(history.getStatus())
                || WorkflowState.FAILED.name().equals(history.getStatus())) {
            log.warn("Cannot cancel workflow [{}] in terminal state [{}]", workflowId, history.getStatus());
            return false;
        }
        history.markCompleted("CANCELLED");
        workflowHistoryRepository.save(history);
        log.info("Workflow [{}] cancelled", workflowId);
        return true;
    }

    /**
     * Lists all failed jobs for an organization.
     */
    public List<FailedJobEntity> listFailedJobs(String orgId) {
        return failedJobRepository.findByOrgId(orgId);
    }

    /**
     * Reprocesses a specific failed job.
     */
    public String reprocessFailedJob(String jobId) {
        var failedJob = failedJobRepository.findById(UUID.fromString(jobId))
                .orElseThrow(() -> new IllegalArgumentException("Failed job not found: " + jobId));

        failedJob.incrementRetryCount();
        failedJobRepository.save(failedJob);

        log.info("Reprocessing failed job [{}] for file [{}], retry count: {}",
                jobId, failedJob.getFileId(), failedJob.getRetryCount());

        return startWorkflow(failedJob.getFileId(), "UNKNOWN", failedJob.getOrgId());
    }

    private List<SagaOrchestrator.SagaStep> buildPipelineSteps(
            StateMachine<WorkflowState, WorkflowEvent> sm,
            String fileId, String workflowId) {

        List<SagaOrchestrator.SagaStep> steps = new ArrayList<>();

        // Step 1: Scan (virus check via atomizer)
        steps.add(new SagaOrchestrator.SagaStep() {
            @Override
            public String name() {
                return "SCAN";
            }

            @Override
            public void execute(SagaOrchestrator.SagaContext ctx) throws Exception {
                var cachedResult = idempotencyService.checkProcessed(ctx.fileId(), "SCAN");
                if (cachedResult.isPresent()) {
                    log.info("Scan step skipped (idempotent) for file [{}]", ctx.fileId());
                    return;
                }

                String atomizerAppId = fileTypeRouter.resolveAtomizerAppId(ctx.fileType());
                Map<String, String> request = Map.of("fileId", ctx.fileId(), "action", "scan");
                directServiceClient.invokeMethod(atomizerAppId, "scan", request, Map.class);

                idempotencyService.markProcessed(ctx.fileId(), "SCAN", "OK");
                sendEvent(sm, WorkflowEvent.SCAN_COMPLETE, fileId, workflowId);
            }

            @Override
            public void compensate(SagaOrchestrator.SagaContext ctx) {
                idempotencyService.invalidate(ctx.fileId(), "SCAN");
                log.info("Scan step compensated for file [{}]", ctx.fileId());
            }
        });

        // Step 2: Parse (atomizer extracts structured data)
        steps.add(new SagaOrchestrator.SagaStep() {
            @Override
            public String name() {
                return "PARSE";
            }

            @Override
            public void execute(SagaOrchestrator.SagaContext ctx) throws Exception {
                var cachedResult = idempotencyService.checkProcessed(ctx.fileId(), "PARSE");
                if (cachedResult.isPresent()) {
                    ctx.put("parseResult", cachedResult.get());
                    log.info("Parse step skipped (idempotent) for file [{}]", ctx.fileId());
                    return;
                }

                String atomizerAppId = fileTypeRouter.resolveAtomizerAppId(ctx.fileType());
                Map<String, String> request = Map.of("fileId", ctx.fileId());

                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = directServiceClient.invokeMethod(
                            atomizerAppId, "parse", request, Map.class);
                    String resultJson = objectMapper.writeValueAsString(response);
                    ctx.put("parseResult", resultJson);
                    idempotencyService.markProcessed(ctx.fileId(), "PARSE", resultJson);
                    sendEvent(sm, WorkflowEvent.PARSE_COMPLETE, fileId, workflowId);
                } catch (Exception e) {
                    throw new ParsingException(ctx.fileId(), "Parsing failed: " + e.getMessage(), e);
                }
            }

            @Override
            public void compensate(SagaOrchestrator.SagaContext ctx) {
                idempotencyService.invalidate(ctx.fileId(), "PARSE");
                log.info("Parse step compensated for file [{}]", ctx.fileId());
            }
        });

        // Step 3: Map (template mapping via engine-data)
        steps.add(new SagaOrchestrator.SagaStep() {
            @Override
            public String name() {
                return "MAP";
            }

            @Override
            public void execute(SagaOrchestrator.SagaContext ctx) throws Exception {
                var cachedResult = idempotencyService.checkProcessed(ctx.fileId(), "MAP");
                if (cachedResult.isPresent()) {
                    ctx.put("mapResult", cachedResult.get());
                    log.info("Map step skipped (idempotent) for file [{}]", ctx.fileId());
                    return;
                }

                String parseResult = ctx.get("parseResult", String.class);
                Map<String, String> request = Map.of(
                        "fileId", ctx.fileId(),
                        "parsedData", parseResult != null ? parseResult : "");

                @SuppressWarnings("unchecked")
                Map<String, Object> response = directServiceClient.invokeMethod(
                        routingConfig.engineData(), "map", request, Map.class);

                String resultJson = objectMapper.writeValueAsString(response);
                ctx.put("mapResult", resultJson);
                idempotencyService.markProcessed(ctx.fileId(), "MAP", resultJson);
                sendEvent(sm, WorkflowEvent.MAP_COMPLETE, fileId, workflowId);
            }

            @Override
            public void compensate(SagaOrchestrator.SagaContext ctx) {
                idempotencyService.invalidate(ctx.fileId(), "MAP");
                log.info("Map step compensated for file [{}]", ctx.fileId());
            }
        });

        // Step 4: Store (sink to table store and document store)
        steps.add(new SagaOrchestrator.SagaStep() {
            @Override
            public String name() {
                return "STORE";
            }

            @Override
            public void execute(SagaOrchestrator.SagaContext ctx) throws Exception {
                var cachedResult = idempotencyService.checkProcessed(ctx.fileId(), "STORE");
                if (cachedResult.isPresent()) {
                    log.info("Store step skipped (idempotent) for file [{}]", ctx.fileId());
                    return;
                }

                String mapResult = ctx.get("mapResult", String.class);
                Map<String, String> request = Map.of(
                        "fileId", ctx.fileId(),
                        "mappedData", mapResult != null ? mapResult : "",
                        "orgId", ctx.orgId());

                try {
                    // Store to table sink (engine-data)
                    directServiceClient.invokeMethod(routingConfig.engineData(), "store", request, Map.class);

                    // Store to document sink (engine-data)
                    directServiceClient.invokeMethod(routingConfig.engineData(), "store-doc", request, Map.class);

                    idempotencyService.markProcessed(ctx.fileId(), "STORE", "OK");
                    sendEvent(sm, WorkflowEvent.STORE_COMPLETE, fileId, workflowId);
                } catch (Exception e) {
                    throw new StorageException(ctx.fileId(), "Storage failed: " + e.getMessage(), e);
                }
            }

            @Override
            public void compensate(SagaOrchestrator.SagaContext ctx) {
                try {
                    Map<String, String> request = Map.of(
                            "fileId", ctx.fileId(),
                            "orgId", ctx.orgId());

                    // Compensate document sink first, then table sink (reverse order)
                    directServiceClient.invokeMethod(routingConfig.engineData(), "rollback-doc", request, Map.class);
                    directServiceClient.invokeMethod(routingConfig.engineData(), "rollback", request, Map.class);

                    idempotencyService.invalidate(ctx.fileId(), "STORE");
                    log.info("Store step compensated for file [{}]", ctx.fileId());
                } catch (Exception e) {
                    log.error("CRITICAL: Store compensation failed for file [{}]: {}",
                            ctx.fileId(), e.getMessage(), e);
                }
            }
        });

        return steps;
    }

    private void sendEvent(StateMachine<WorkflowState, WorkflowEvent> sm,
            WorkflowEvent event, String fileId, String workflowId) {
        Message<WorkflowEvent> message = MessageBuilder.withPayload(event)
                .setHeader(FILE_ID_HEADER, fileId)
                .setHeader(WORKFLOW_ID_HEADER, workflowId)
                .build();
        sm.sendEvent(reactor.core.publisher.Mono.just(message)).blockLast();
    }

    private String serializeSteps(List<String> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize steps list", e);
            return "[]";
        }
    }
}
