package com.reportplatform.qry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for querying form data via MS-FORM gRPC.
 * Acts as a bridge between MS-QRY (frontend-facing) and MS-FORM (internal
 * service).
 */
@Service
public class FormQueryService {

    private static final Logger log = LoggerFactory.getLogger(FormQueryService.class);

    private final DaprClient daprClient;
    private final String msFormAppId;
    private final ObjectMapper objectMapper;

    public FormQueryService(
            DaprClient daprClient,
            @Value("${dapr.remote.ms-form-app-id:ms-form}") String msFormAppId) {
        this.daprClient = daprClient;
        this.msFormAppId = msFormAppId;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns aggregated form data for a specific form.
     * Calls MS-FORM via Dapr service invocation to get form response statistics.
     */
    public Map<String, Object> getFormData(UUID formId, String orgId) {
        log.info("Querying form data for formId={}, orgId={}", formId, orgId);

        try {
            // Build request to MS-FORM
            Map<String, String> request = new HashMap<>();
            request.put("formId", formId.toString());
            request.put("orgId", orgId);

            // Call MS-FORM via Dapr service invocation
            Map<String, Object> response = daprClient.invokeMethod(
                    msFormAppId,
                    "/api/forms/" + formId + "/stats",
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            if (response != null) {
                response.put("sourceType", "FORM");
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to query MS-FORM for form data: {}", e.getMessage());
        }

        // Fallback: return response with defaults if MS-FORM call fails
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("formId", formId.toString());
        fallback.put("sourceType", "FORM");
        fallback.put("totalResponses", 0);
        fallback.put("submittedCount", 0);
        fallback.put("draftCount", 0);
        fallback.put("completionRate", 0.0);
        fallback.put("lastUpdated", new Date().toString());
        fallback.put("_fallback", true);

        return fallback;
    }

    /**
     * Returns paginated form responses for a form.
     */
    public Map<String, Object> getFormResponses(UUID formId, String orgId,
            String periodId, String status, int page, int size) {

        log.info("Querying form responses: formId={}, orgId={}, periodId={}, status={}",
                formId, orgId, periodId, status);

        try {
            // Build query parameters
            StringBuilder path = new StringBuilder("/api/forms/");
            path.append(formId);
            path.append("/responses?page=").append(page);
            path.append("&size=").append(size);

            if (periodId != null) {
                path.append("&periodId=").append(periodId);
            }
            if (status != null) {
                path.append("&status=").append(status);
            }

            Map<String, String> request = new HashMap<>();
            request.put("orgId", orgId);

            // Call MS-FORM via Dapr service invocation
            Map<String, Object> response = daprClient.invokeMethod(
                    msFormAppId,
                    path.toString(),
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            if (response != null) {
                response.put("sourceType", "FORM");
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to query MS-FORM for form responses: {}", e.getMessage());
        }

        // Fallback response
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("formId", formId.toString());
        fallback.put("sourceType", "FORM");
        fallback.put("responses", new ArrayList<>());
        fallback.put("page", page);
        fallback.put("size", size);
        fallback.put("totalElements", 0);
        fallback.put("totalPages", 0);
        fallback.put("_fallback", true);

        return fallback;
    }

    /**
     * Returns a specific organization's form response.
     */
    public Optional<Map<String, Object>> getOrgFormResponse(UUID formId, String orgId) {
        log.info("Querying org form response: formId={}, orgId={}", formId, orgId);

        try {
            Map<String, String> request = new HashMap<>();
            request.put("orgId", orgId);

            // Call MS-FORM via Dapr service invocation
            Map<String, Object> response = daprClient.invokeMethod(
                    msFormAppId,
                    "/api/forms/" + formId + "/org/" + orgId,
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            if (response != null) {
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.warn("Failed to query MS-FORM for org form response: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Returns form completion status for a period.
     * Used by matrix dashboards.
     */
    public List<Map<String, Object>> getFormCompletions(String periodId, String orgId) {
        log.info("Querying form completions: periodId={}, orgId={}", periodId, orgId);

        try {
            Map<String, String> request = new HashMap<>();
            request.put("orgId", orgId);
            request.put("periodId", periodId);

            // Call MS-FORM via Dapr service invocation
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = (List<Map<String, Object>>) daprClient.invokeMethod(
                    msFormAppId,
                    "/api/forms/completions?periodId=" + periodId,
                    request,
                    HttpExtension.POST,
                    new TypeRef<List<Map<String, Object>>>() {
                    })
                    .block();

            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to query MS-FORM for form completions: {}", e.getMessage());
        }

        // Return empty list as fallback
        return new ArrayList<>();
    }
}
