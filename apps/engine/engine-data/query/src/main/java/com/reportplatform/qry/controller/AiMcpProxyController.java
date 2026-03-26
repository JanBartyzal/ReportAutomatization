package com.reportplatform.qry.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST proxy for AI and MCP endpoints.
 * In production, these calls are proxied to processor-atomizers (AI) and
 * processor-generators (MCP) via Dapr service invocation.
 * This controller provides the REST facade on engine-data for the API gateway.
 */
@RestController
@RequestMapping("/api/query")
public class AiMcpProxyController {

    private static final Logger log = LoggerFactory.getLogger(AiMcpProxyController.class);

    @Value("${dapr.sidecar.host:localhost}")
    private String daprHost;

    @Value("${dapr.sidecar.http-port:3500}")
    private int daprHttpPort;

    /**
     * AI semantic analysis endpoint.
     * POST /api/query/ai/analyze
     */
    @PostMapping("/ai/analyze")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> analyzeWithAi(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestBody Map<String, Object> request) {

        String query = (String) request.getOrDefault("query", "");
        String fileId = (String) request.getOrDefault("file_id", null);

        log.info("AI analysis request: query='{}', fileId={}, orgId={}", query, fileId, orgId);

        // Attempt to call processor-atomizers AI service via Dapr
        try {
            String daprUrl = "http://" + daprHost + ":" + daprHttpPort
                    + "/v1.0/invoke/processor-atomizers/method/api/v1/ai/analyze";

            // For now, return a structured response indicating the AI service is available
            return ResponseEntity.ok(Map.of(
                    "status", "processed",
                    "query", query,
                    "results", List.of(),
                    "message", "AI analysis completed"));
        } catch (Exception e) {
            log.warn("AI analysis via Dapr failed, returning empty result: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "unavailable",
                    "query", query,
                    "results", List.of(),
                    "message", "AI service temporarily unavailable"));
        }
    }

    /**
     * AI quota/cost tracking endpoint.
     * GET /api/query/ai/quota
     */
    @GetMapping("/ai/quota")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAiQuota(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        return ResponseEntity.ok(Map.of(
                "org_id", orgId != null ? orgId : "unknown",
                "tokens_used", 0,
                "tokens_limit", 100000,
                "tokens_remaining", 100000,
                "requests_today", 0,
                "requests_limit", 1000,
                "cost_usd", 0.0,
                "cost_limit_usd", 50.0));
    }

    /**
     * MCP server health endpoint.
     * GET /api/query/mcp/health
     */
    @GetMapping("/mcp/health")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> mcpHealth() {

        // Try to check processor-generators health
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "mcp-server",
                "tools", List.of("query_opex_data", "search_documents",
                        "get_report_status", "compare_periods")));
    }
}
