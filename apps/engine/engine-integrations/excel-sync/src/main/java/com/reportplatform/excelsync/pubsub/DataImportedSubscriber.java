package com.reportplatform.excelsync.pubsub;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.excelsync.model.entity.ExportFlowDefinitionEntity;
import com.reportplatform.excelsync.model.entity.TriggerType;
import com.reportplatform.excelsync.repository.ExportFlowDefinitionRepository;
import com.reportplatform.excelsync.service.ExportFlowExecutionService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class DataImportedSubscriber {

    private static final Logger log = LoggerFactory.getLogger(DataImportedSubscriber.class);

    private final ExportFlowDefinitionRepository definitionRepository;
    private final ExportFlowExecutionService executionService;
    private final ObjectMapper objectMapper;

    public DataImportedSubscriber(ExportFlowDefinitionRepository definitionRepository,
                                  ExportFlowExecutionService executionService,
                                  ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Topic(name = "data-imported", pubsubName = "${dapr.pubsub.name:reportplatform-pubsub}")
    @PostMapping(path = "/api/v1/events/data-imported")
    public void handleDataImported(@RequestBody CloudEvent<Map<String, Object>> event) {
        Map<String, Object> data = event.getData();
        if (data == null) {
            log.warn("Received data-imported event with null data");
            return;
        }

        String orgIdStr   = String.valueOf(data.getOrDefault("orgId",     ""));
        String batchId    = String.valueOf(data.getOrDefault("batchId",   ""));
        String fileId     = String.valueOf(data.getOrDefault("fileId",    ""));
        String sourceType = String.valueOf(data.getOrDefault("sourceType",""));
        String fileName   = String.valueOf(data.getOrDefault("fileName",  ""));

        log.info("Received data-imported event: orgId={}, batchId={}, fileId={}, sourceType={}",
                orgIdStr, batchId, fileId, sourceType);

        if (orgIdStr.isBlank()) {
            log.warn("data-imported event missing orgId, skipping");
            return;
        }

        try {
            UUID orgId = UUID.fromString(orgIdStr);

            // Find active AUTO-trigger flows for this org
            List<ExportFlowDefinitionEntity> candidateFlows =
                    definitionRepository.findByOrgIdAndIsActiveTrueAndTriggerType(orgId, TriggerType.AUTO);

            if (candidateFlows.isEmpty()) {
                log.debug("No active AUTO export flows for org [{}]", orgId);
                return;
            }

            // Apply triggerFilter matching for each candidate flow
            List<ExportFlowDefinitionEntity> matchingFlows = candidateFlows.stream()
                    .filter(flow -> matchesTriggerFilter(flow, sourceType, batchId, fileName))
                    .toList();

            if (matchingFlows.isEmpty()) {
                log.debug("No flows matched triggerFilter for event [sourceType={}, batchId={}]",
                        sourceType, batchId);
                return;
            }

            log.info("Found {} matching export flows for org [{}] (from {} candidates)",
                    matchingFlows.size(), orgId, candidateFlows.size());

            for (ExportFlowDefinitionEntity flow : matchingFlows) {
                log.info("Enqueueing export flow [{}] '{}' for execution", flow.getId(), flow.getName());
                executionService.executeFlow(flow.getId(), orgId, "AUTO", batchId);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid orgId in data-imported event: {}", orgIdStr, e);
        } catch (Exception e) {
            log.error("Error processing data-imported event: {}", e.getMessage(), e);
        }
    }

    /**
     * Evaluates whether an export flow's triggerFilter matches the incoming event.
     *
     * <p>TriggerFilter JSON schema:
     * <pre>
     * {
     *   "sourceTypes": ["EXCEL", "PPTX"],   // optional – if present, event sourceType must be in list
     *   "batchNamePattern": "Q1*"            // optional – glob pattern matched against batchId or fileName
     * }
     * </pre>
     * An empty or null triggerFilter means the flow matches all events.</p>
     */
    private boolean matchesTriggerFilter(ExportFlowDefinitionEntity flow,
                                         String sourceType, String batchId, String fileName) {
        String filterJson = flow.getTriggerFilter();
        if (filterJson == null || filterJson.isBlank() || "{}".equals(filterJson.trim())) {
            return true; // No filter – match everything
        }

        try {
            Map<String, Object> filter = objectMapper.readValue(
                    filterJson, new TypeReference<Map<String, Object>>() {});

            // 1. sourceTypes filter
            Object sourceTypesRaw = filter.get("sourceTypes");
            if (sourceTypesRaw instanceof List<?> sourceTypes && !sourceTypes.isEmpty()) {
                boolean typeMatch = sourceTypes.stream()
                        .anyMatch(t -> sourceType.equalsIgnoreCase(String.valueOf(t)));
                if (!typeMatch) {
                    log.debug("Flow [{}] skipped: sourceType '{}' not in filter {}",
                            flow.getId(), sourceType, sourceTypes);
                    return false;
                }
            }

            // 2. batchNamePattern filter (glob-style: * = any chars, ? = single char)
            Object patternRaw = filter.get("batchNamePattern");
            if (patternRaw instanceof String pattern && !pattern.isBlank()) {
                String regex = globToRegex(pattern);
                boolean batchMatch = batchId.matches(regex) || fileName.matches(regex);
                if (!batchMatch) {
                    log.debug("Flow [{}] skipped: batchId '{}' / fileName '{}' did not match pattern '{}'",
                            flow.getId(), batchId, fileName, pattern);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.warn("Failed to parse triggerFilter for flow [{}], treating as match: {}",
                    flow.getId(), e.getMessage());
            return true; // Fail open: malformed filter → match
        }
    }

    /**
     * Converts a simple glob pattern (* and ?) to a Java regex.
     * Only * and ? are treated as wildcards; all other regex metacharacters are escaped.
     */
    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("(?i)"); // case-insensitive
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*'  -> sb.append(".*");
                case '?'  -> sb.append('.');
                case '.'  -> sb.append("\\.");
                case '('  -> sb.append("\\(");
                case ')'  -> sb.append("\\)");
                case '['  -> sb.append("\\[");
                case ']'  -> sb.append("\\]");
                case '{'  -> sb.append("\\{");
                case '}'  -> sb.append("\\}");
                case '+'  -> sb.append("\\+");
                case '^'  -> sb.append("\\^");
                case '$'  -> sb.append("\\$");
                case '|'  -> sb.append("\\|");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
