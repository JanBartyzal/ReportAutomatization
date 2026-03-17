package com.reportplatform.dash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.dash.model.dto.DashboardGeneratePptxRequest;
import com.reportplatform.dash.model.dto.DashboardGeneratePptxResponse;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating PPTX reports from dashboards.
 * Uses Dapr gRPC to communicate with MS-GEN-PPTX.
 */
@Service
public class DashboardPptxService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardPptxService.class);
    private static final String MS_GEN_PPTX_APP_ID = "ms-gen-pptx";
    private static final String JOB_STATUS_PREFIX = "pptx_generation:";
    private static final long JOB_STATUS_TTL_SECONDS = 3600; // 1 hour

    private final DashboardService dashboardService;
    private final AggregationService aggregationService;
    private final DaprClient daprClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.generation.timeout-seconds:300}")
    private int generationTimeoutSeconds;

    @Value("${app.generation.default-template-id:default-dashboard-template}")
    private String defaultTemplateId;

    public DashboardPptxService(DashboardService dashboardService,
            AggregationService aggregationService,
            DaprClient daprClient,
            RedisTemplate<String, Object> redisTemplate) {
        this.dashboardService = dashboardService;
        this.aggregationService = aggregationService;
        this.daprClient = daprClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate a PPTX report from a dashboard.
     * This is an async operation that triggers the generation and returns a job ID.
     */
    public DashboardGeneratePptxResponse generatePptx(String orgId, DashboardGeneratePptxRequest request) {
        logger.info("Starting PPTX generation for dashboard: {}", request.dashboardId());

        // Validate dashboard exists
        var dashboard = dashboardService.getDashboard(
                UUID.fromString(request.dashboardId()),
                UUID.fromString(orgId));

        // Generate a job ID for tracking
        String jobId = UUID.randomUUID().toString();

        try {
            // Get template ID from request or use default
            String templateId = request.templateId() != null ? request.templateId() : defaultTemplateId;

            // Get dashboard data
            var dashboardData = aggregationService.getDashboardData(
                    UUID.fromString(orgId),
                    UUID.fromString(request.dashboardId()));

            // Prepare request for MS-GEN-PPTX
            Map<String, Object> generationRequest = new HashMap<>();
            generationRequest.put("job_id", jobId);
            generationRequest.put("dashboard_id", request.dashboardId());
            generationRequest.put("org_id", orgId);
            generationRequest.put("template_id", templateId);
            generationRequest.put("dashboard_data", dashboardData);
            generationRequest.put("callback_url", "/api/dashboards/generation/callback");

            // Store initial job status in Redis
            Map<String, Object> jobStatus = new HashMap<>();
            jobStatus.put("job_id", jobId);
            jobStatus.put("status", "PROCESSING");
            jobStatus.put("dashboard_id", request.dashboardId());
            jobStatus.put("template_id", templateId);
            jobStatus.put("created_at", Instant.now().toString());
            jobStatus.put("progress", 0);
            redisTemplate.opsForValue().set(JOB_STATUS_PREFIX + jobId, jobStatus,
                    Duration.ofSeconds(JOB_STATUS_TTL_SECONDS));

            // Call MS-GEN-PPTX via Dapr gRPC
            try {
                Map<String, Object> response = daprClient.invokeMethod(
                        MS_GEN_PPTX_APP_ID,
                        "/api/v1/generate",
                        generationRequest,
                        HttpExtension.POST,
                        Map.class).block();

                if (response != null) {
                    logger.info("MS-GEN-PPTX accepted generation job {}", jobId);
                }
            } catch (Exception e) {
                logger.warn("Failed to invoke MS-GEN-PPTX: {}, job will be tracked locally", e.getMessage());
                // Continue with local tracking fallback
            }

            logger.info("PPTX generation job {} created for dashboard {}", jobId, request.dashboardId());

            return new DashboardGeneratePptxResponse(
                    jobId,
                    "PROCESSING",
                    "PPTX generation started. Use the job ID to check status.",
                    null, // Download URL will be available when complete
                    Instant.now().toString());

        } catch (Exception e) {
            logger.error("Failed to start PPTX generation: {}", e.getMessage(), e);

            // Update job status to FAILED
            try {
                Map<String, Object> jobStatus = new HashMap<>();
                jobStatus.put("job_id", jobId);
                jobStatus.put("status", "FAILED");
                jobStatus.put("error", e.getMessage());
                jobStatus.put("created_at", Instant.now().toString());
                redisTemplate.opsForValue().set(JOB_STATUS_PREFIX + jobId, jobStatus,
                        Duration.ofSeconds(JOB_STATUS_TTL_SECONDS));
            } catch (Exception redisError) {
                logger.warn("Failed to update job status in Redis: {}", redisError.getMessage());
            }

            return new DashboardGeneratePptxResponse(
                    jobId,
                    "FAILED",
                    "Failed to start PPTX generation: " + e.getMessage(),
                    null,
                    Instant.now().toString());
        }
    }

    /**
     * Check the status of a generation job.
     */
    public DashboardGeneratePptxResponse getGenerationStatus(String jobId) {
        try {
            // Try to get status from Redis
            Object cachedStatus = redisTemplate.opsForValue().get(JOB_STATUS_PREFIX + jobId);

            if (cachedStatus instanceof Map) {
                Map<String, Object> status = (Map<String, Object>) cachedStatus;
                String statusStr = (String) status.getOrDefault("status", "UNKNOWN");
                String error = (String) status.get("error");
                Integer progress = (Integer) status.getOrDefault("progress", 0);

                String message;
                if ("PROCESSING".equals(statusStr)) {
                    message = String.format("PPTX generation in progress... %d%%", progress);
                } else if ("FAILED".equals(statusStr)) {
                    message = error != null ? error : "Generation failed";
                } else if ("COMPLETED".equals(statusStr)) {
                    message = "PPTX generation completed successfully";
                } else {
                    message = (String) status.getOrDefault("message", "Job status: " + statusStr);
                }

                String downloadUrl = (String) status.get("download_url");

                return new DashboardGeneratePptxResponse(
                        jobId,
                        statusStr,
                        message,
                        downloadUrl,
                        (String) status.get("created_at"));
            }
        } catch (Exception e) {
            logger.warn("Failed to get job status from Redis: {}", e.getMessage());
        }

        // Fallback: try to call MS-GEN-PPTX directly
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("job_id", jobId);

            Map<String, Object> response = daprClient.invokeMethod(
                    MS_GEN_PPTX_APP_ID,
                    "/api/v1/status",
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            if (response != null) {
                String status = (String) response.getOrDefault("status", "UNKNOWN");
                String downloadUrl = (String) response.get("download_url");
                String message = (String) response.getOrDefault("message", "Status: " + status);

                return new DashboardGeneratePptxResponse(
                        jobId,
                        status,
                        message,
                        downloadUrl,
                        null);
            }
        } catch (Exception e) {
            logger.warn("Failed to get status from MS-GEN-PPTX: {}", e.getMessage());
        }

        return new DashboardGeneratePptxResponse(
                jobId,
                "NOT_FOUND",
                "Job not found in cache or MS-GEN-PPTX. It may have expired.",
                null,
                null);
    }

    /**
     * Get generated PPTX download URL.
     */
    public String getDownloadUrl(String jobId) {
        try {
            // Try Redis first
            Object cachedStatus = redisTemplate.opsForValue().get(JOB_STATUS_PREFIX + jobId);

            if (cachedStatus instanceof Map) {
                Map<String, Object> status = (Map<String, Object>) cachedStatus;
                String statusStr = (String) status.get("status");

                if ("COMPLETED".equals(statusStr)) {
                    return (String) status.get("download_url");
                }
            }

            // Fallback: call MS-GEN-PPTX
            Map<String, Object> request = new HashMap<>();
            request.put("job_id", jobId);

            Map<String, Object> response = daprClient.invokeMethod(
                    MS_GEN_PPTX_APP_ID,
                    "/api/v1/download",
                    request,
                    HttpExtension.POST,
                    Map.class).block();

            if (response != null) {
                String downloadUrl = (String) response.get("download_url");
                if (downloadUrl != null) {
                    return downloadUrl;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get download URL: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Update job status (called by callback from MS-GEN-PPTX).
     */
    public void updateJobStatus(String jobId, String status, String downloadUrl, String error) {
        try {
            Map<String, Object> jobStatus = new HashMap<>();
            jobStatus.put("job_id", jobId);
            jobStatus.put("status", status);
            jobStatus.put("updated_at", Instant.now().toString());

            if (downloadUrl != null) {
                jobStatus.put("download_url", downloadUrl);
            }
            if (error != null) {
                jobStatus.put("error", error);
            }

            redisTemplate.opsForValue().set(JOB_STATUS_PREFIX + jobId, jobStatus,
                    Duration.ofSeconds(JOB_STATUS_TTL_SECONDS));
            logger.info("Updated job {} status to {}", jobId, status);
        } catch (Exception e) {
            logger.error("Failed to update job status: {}", e.getMessage());
        }
    }
}
