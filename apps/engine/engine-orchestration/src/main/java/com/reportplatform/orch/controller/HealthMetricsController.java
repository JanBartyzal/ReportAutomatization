package com.reportplatform.orch.controller;

import com.reportplatform.orch.dto.HealthMetricsResponse;
import com.reportplatform.orch.service.HealthMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint exposing aggregated health metrics from the orchestrator.
 * Called by engine-core admin module via Dapr service invocation.
 */
@RestController
@RequestMapping("/api/v1/health-metrics")
public class HealthMetricsController {

    private final HealthMetricsService healthMetricsService;

    public HealthMetricsController(HealthMetricsService healthMetricsService) {
        this.healthMetricsService = healthMetricsService;
    }

    @GetMapping
    public ResponseEntity<HealthMetricsResponse> getMetrics() {
        return ResponseEntity.ok(healthMetricsService.getMetrics());
    }
}
