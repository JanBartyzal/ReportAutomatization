package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for generating PPTX from a dashboard.
 */
public record DashboardGeneratePptxRequest(
        @NotBlank(message = "Dashboard ID is required")
        String dashboardId,
        
        String templateId,  // Optional: specific template to use
        
        String title,  // Optional: custom title for the report
        
        String period  // Optional: specific period to generate for
) {}
