package com.reportplatform.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.admin.model.dto.*;
import com.reportplatform.admin.model.entity.HealthServiceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Aggregates health data from all services (actuator probes) and
 * workflow metrics from the orchestrator (via Dapr service invocation).
 * Service URLs are read from the database at each request (no restart needed).
 */
@Service
public class HealthDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(HealthDashboardService.class);
    private static final long PROBE_TIMEOUT_SECONDS = 3;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HealthServiceRegistryService registryService;

    @Value("${dapr.host:localhost}")
    private String daprHost;

    @Value("${dapr.http-port:3500}")
    private int daprPort;

    @Value("${health.grafana-url:http://localhost:3000/grafana}")
    private String grafanaUrl;

    public HealthDashboardService(ObjectMapper objectMapper,
                                  HealthServiceRegistryService registryService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.registryService = registryService;
    }

    public HealthDashboardDTO getDashboard() {
        List<ServiceHealthDTO> services = probeAllServices();
        SystemMetricsDTO metrics = fetchMetricsFromOrchestrator();
        List<ErrorLogEntryDTO> errors = fetchRecentErrorsFromOrchestrator();

        return new HealthDashboardDTO(
                services,
                metrics,
                errors,
                grafanaUrl,
                Instant.now().toString()
        );
    }

    private List<ServiceHealthDTO> probeAllServices() {
        List<HealthServiceEntity> enabledServices = registryService.getEnabledServices();

        if (enabledServices.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<ServiceHealthDTO>> futures = enabledServices.stream()
                .map(svc -> CompletableFuture.supplyAsync(() ->
                        probeService(svc.getServiceId(), svc.getDisplayName(), svc.getHealthUrl()))
                        .orTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> buildDownResponse(svc.getServiceId(), svc.getDisplayName()))
                )
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private ServiceHealthDTO probeService(String serviceId, String displayName, String url) {
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            long responseTime = System.currentTimeMillis() - start;

            String status = "down";
            String version = "unknown";

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String actuatorStatus = root.path("status").asText("UNKNOWN");
                status = mapActuatorStatus(actuatorStatus);
                version = extractVersion(root);
            }

            return new ServiceHealthDTO(
                    serviceId, displayName, status,
                    Instant.now().toString(), responseTime,
                    "healthy".equals(status) ? 99.9 : 0.0,
                    version, 0
            );
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            logger.warn("Health probe failed for {}: {}", serviceId, e.getMessage());
            return new ServiceHealthDTO(
                    serviceId, displayName, "down",
                    Instant.now().toString(), responseTime,
                    0.0, "unknown", 0
            );
        }
    }

    private ServiceHealthDTO buildDownResponse(String serviceId, String displayName) {
        return new ServiceHealthDTO(
                serviceId, displayName, "down",
                Instant.now().toString(), 0, 0.0, "unknown", 0
        );
    }

    /**
     * Extracts version from health response, trying multiple paths:
     * 1. Spring Boot actuator: components.moduleHealth.details.version
     * 2. Python FastAPI: root.version
     * 3. Any component with a "version" detail
     */
    private String extractVersion(JsonNode root) {
        // 1) Spring Boot actuator — components.moduleHealth.details.version
        JsonNode moduleVersion = root.path("components").path("moduleHealth").path("details").path("version");
        if (!moduleVersion.isMissingNode() && !moduleVersion.asText("").isEmpty()) {
            return moduleVersion.asText();
        }

        // 2) Direct "version" field (Python services, simple health endpoints)
        JsonNode directVersion = root.path("version");
        if (!directVersion.isMissingNode() && !directVersion.asText("").isEmpty()) {
            return directVersion.asText();
        }

        // 3) Scan all actuator components for any "version" detail
        JsonNode components = root.path("components");
        if (components.isObject()) {
            var fields = components.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                JsonNode v = entry.getValue().path("details").path("version");
                if (!v.isMissingNode() && !v.asText("").isEmpty()) {
                    return v.asText();
                }
            }
        }

        return "unknown";
    }

    private String mapActuatorStatus(String actuatorStatus) {
        return switch (actuatorStatus.toUpperCase()) {
            case "UP", "HEALTHY" -> "healthy";
            case "DOWN", "OUT_OF_SERVICE" -> "down";
            default -> "degraded";
        };
    }

    private SystemMetricsDTO fetchMetricsFromOrchestrator() {
        String url = String.format(
                "http://%s:%d/v1.0/invoke/ms-orch/method/api/v1/health-metrics",
                daprHost, daprPort
        );

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return new SystemMetricsDTO(
                        root.path("activeWorkflows").asLong(0),
                        root.path("dlqDepth").asLong(0),
                        root.path("totalProcessed").asLong(0),
                        root.path("failedJobs").asLong(0),
                        root.path("avgProcessingTimeMs").asDouble(0.0)
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch metrics from orchestrator: {}", e.getMessage());
        }

        return new SystemMetricsDTO(0, 0, 0, 0, 0.0);
    }

    private List<ErrorLogEntryDTO> fetchRecentErrorsFromOrchestrator() {
        String url = String.format(
                "http://%s:%d/v1.0/invoke/ms-orch/method/api/v1/health-metrics",
                daprHost, daprPort
        );

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode errors = root.path("recentErrors");

                List<ErrorLogEntryDTO> result = new ArrayList<>();
                for (JsonNode err : errors) {
                    int retryCount = err.path("retryCount").asInt(0);
                    String level = retryCount >= 3 ? "critical" : "error";

                    result.add(new ErrorLogEntryDTO(
                            err.path("id").asText(),
                            err.path("timestamp").asText(),
                            "ms-orch",
                            level,
                            err.path("errorType").asText(),
                            err.path("errorDetail").asText()
                    ));
                }
                return result;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch recent errors from orchestrator: {}", e.getMessage());
        }

        return List.of();
    }
}
