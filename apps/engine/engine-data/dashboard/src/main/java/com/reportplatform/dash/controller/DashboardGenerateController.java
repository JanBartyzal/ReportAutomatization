package com.reportplatform.dash.controller;

import com.reportplatform.dash.model.dto.DashboardGeneratePptxRequest;
import com.reportplatform.dash.model.dto.DashboardGeneratePptxResponse;
import com.reportplatform.dash.service.DashboardPptxService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dashboard PPTX generation endpoints.
 */
@RestController
@RequestMapping("/api/dashboards")
public class DashboardGenerateController {

    private final DashboardPptxService dashboardPptxService;

    public DashboardGenerateController(DashboardPptxService dashboardPptxService) {
        this.dashboardPptxService = dashboardPptxService;
    }

    /**
     * Generate a PPTX report from a dashboard.
     * POST /api/dashboards/generate-pptx
     */
    @PostMapping("/generate-pptx")
    public ResponseEntity<DashboardGeneratePptxResponse> generatePptx(
            @RequestHeader("X-Org-Id") String orgId,
            @Valid @RequestBody DashboardGeneratePptxRequest request) {

        var response = dashboardPptxService.generatePptx(orgId, request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get the status of a PPTX generation job.
     * GET /api/dashboards/generate-pptx/{jobId}
     */
    @GetMapping("/generate-pptx/{jobId}")
    public ResponseEntity<DashboardGeneratePptxResponse> getGenerationStatus(
            @PathVariable String jobId) {

        var response = dashboardPptxService.getGenerationStatus(jobId);
        return ResponseEntity.ok(response);
    }

    /**
     * Download a generated PPTX report.
     * GET /api/dashboards/generate-pptx/{jobId}/download
     */
    @GetMapping("/generate-pptx/{jobId}/download")
    public ResponseEntity<byte[]> downloadPptx(@PathVariable String jobId) {
        // First get the download URL from the service
        String downloadUrl = dashboardPptxService.getDownloadUrl(jobId);

        if (downloadUrl == null) {
            // Check if job exists and is completed
            var status = dashboardPptxService.getGenerationStatus(jobId);
            if ("PROCESSING".equals(status.status())) {
                return ResponseEntity.status(202)
                        .header("Content-Type", "text/plain")
                        .body("Generation still in progress".getBytes());
            } else if ("FAILED".equals(status.status())) {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "text/plain")
                        .body(("Generation failed: " + status.message()).getBytes());
            }
            return ResponseEntity.notFound().build();
        }

        try {
            // Download file from blob storage
            java.net.URI uri = new java.net.URI(downloadUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            java.net.http.HttpResponse<byte[]> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                byte[] fileContent = response.body();
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"dashboard-report-" + jobId + ".pptx\"")
                        .header("Content-Type",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                        .body(fileContent);
            } else {
                return ResponseEntity.status(response.statusCode())
                        .header("Content-Type", "text/plain")
                        .body(("Failed to download file: HTTP " + response.statusCode()).getBytes());
            }
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(DashboardGenerateController.class);
            logger.error("Failed to download PPTX file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "text/plain")
                    .body(("Failed to download file: " + e.getMessage()).getBytes());
        }
    }
}
