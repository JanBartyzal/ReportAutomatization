package com.reportplatform.template.tmpl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for calling MS-ATM-AI via Dapr gRPC to get AI-assisted mapping
 * suggestions.
 *
 * Uses the SuggestCleaning RPC from AiGatewayService to get column name
 * normalization suggestions, which are then converted to mapping actions.
 */
@Service
public class AiMappingClient {

    private static final Logger logger = LoggerFactory.getLogger(AiMappingClient.class);

    @Value("${mapping.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${mapping.ai.min-confidence:0.8}")
    private double minConfidence;

    @Value("${mapping.ai.dapr-app-id:ms-atm-ai}")
    private String aiDaprAppId;

    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;

    public AiMappingClient(DaprClient daprClient) {
        this.daprClient = daprClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Request AI-assisted mapping suggestions for the given source headers.
     *
     * Calls MS-ATM-AI's SuggestCleaning RPC via Dapr service invocation.
     * Only returns suggestions with confidence >= min-confidence threshold.
     *
     * @param sourceHeaders Column headers to suggest mappings for
     * @param orgId         Organization ID for quota tracking
     * @return List of AI-suggested mapping actions
     */
    public List<MappingActionData> suggestMappingsViaAi(List<String> sourceHeaders, String orgId) {
        if (!aiEnabled) {
            logger.debug("AI mapping disabled, skipping AI suggestions");
            return List.of();
        }

        if (sourceHeaders == null || sourceHeaders.isEmpty()) {
            logger.debug("No source headers provided, skipping AI suggestions");
            return List.of();
        }

        try {
            logger.info("AI mapping suggestion requested for org={} headers={}", orgId, sourceHeaders.size());

            // Build CleaningRequest proto with headers and empty sample_rows
            Map<String, Object> request = new HashMap<>();
            request.put("headers", sourceHeaders);
            request.put("sample_rows", List.of());
            request.put("org_id", orgId);

            // Call ms-atm-ai via DaprClient.invokeMethod
            Map<String, Object> response = daprClient.invokeMethod(
                    aiDaprAppId,
                    "/api/v1/ai/suggest-cleaning",
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            if (response == null) {
                logger.warn("AI mapping suggestion returned null response");
                return List.of();
            }

            // Parse CleaningResponse and convert ColumnSuggestion to MappingActionData
            List<MappingActionData> suggestions = new ArrayList<>();

            Object suggestionsObj = response.get("suggestions");
            if (suggestionsObj instanceof List) {
                List<Map<String, Object>> suggestionsList = (List<Map<String, Object>>) suggestionsObj;

                for (Map<String, Object> suggestion : suggestionsList) {
                    double confidence = 0.0;
                    Object confidenceObj = suggestion.get("confidence");
                    if (confidenceObj instanceof Number) {
                        confidence = ((Number) confidenceObj).doubleValue();
                    }

                    // Filter by min-confidence threshold
                    if (confidence < minConfidence) {
                        logger.debug("Skipping suggestion with confidence {} below threshold {}",
                                confidence, minConfidence);
                        continue;
                    }

                    String originalColumn = (String) suggestion.get("original_column");
                    String suggestedMapping = (String) suggestion.get("suggested_mapping");
                    String actionType = (String) suggestion.getOrDefault("action_type", "MAP_TO");

                    if (originalColumn != null && suggestedMapping != null) {
                        MappingActionData actionData = new MappingActionData(
                                originalColumn,
                                suggestedMapping,
                                actionType,
                                confidence);
                        suggestions.add(actionData);
                        logger.debug("AI suggested mapping: {} -> {} (confidence: {})",
                                originalColumn, suggestedMapping, confidence);
                    }
                }
            }

            logger.info("AI mapping returned {} suggestions for org={}", suggestions.size(), orgId);
            return suggestions;

        } catch (Exception e) {
            logger.warn("AI mapping suggestion failed for org={}: {}", orgId, e.getMessage());
            return List.of();
        }
    }
}
