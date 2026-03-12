package com.reportplatform.dash.model.dto;

/**
 * Response DTO for dashboard PPTX generation.
 */
public record DashboardGeneratePptxResponse(
        String jobId,
        String status,
        String message,
        String downloadUrl,
        String generatedAt
) {}
