package com.reportplatform.qry.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

/**
 * REST proxy for AI and MCP endpoints.
 * Forwards calls to processor-atomizers (AI) and processor-generators (MCP)
 * via Dapr HTTP service invocation (http://{daprHost}:{daprHttpPort}/v1.0/invoke/{appId}/method/{path}).
 */
@RestController
@RequestMapping("/api/query")
public class AiMcpProxyController {

    private static final Logger log = LoggerFactory.getLogger(AiMcpProxyController.class);

    private static final String APP_ATOMIZERS   = "processor-atomizers";
    private static final String APP_GENERATORS  = "processor-generators";

    @Value("${dapr.sidecar.host:localhost}")
    private String daprHost;

    @Value("${dapr.sidecar.http-port:3500}")
    private int daprHttpPort;

    @Value("${dapr.proxy.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${dapr.proxy.read-timeout-seconds:30}")
    private int readTimeoutSeconds;

    private final RestTemplateBuilder restTemplateBuilder;
    private RestTemplate restTemplate;

    public AiMcpProxyController(RestTemplateBuilder builder) {
        this.restTemplateBuilder = builder;
    }

    @jakarta.annotation.PostConstruct
    void init() {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }

    /**
     * AI semantic analysis — proxied to processor-atomizers via Dapr.
     * POST /api/query/ai/analyze
     */
    @PostMapping("/ai/analyze")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> analyzeWithAi(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestBody Map<String, Object> request) {

        String query  = (String) request.getOrDefault("query", "");
        String fileId = (String) request.getOrDefault("file_id", null);
        log.info("AI analysis request: query='{}', fileId={}, orgId={}", query, fileId, orgId);

        String url = daprInvokeUrl(APP_ATOMIZERS, "/api/v1/ai/analyze");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(request),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("AI analysis call returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", "AI service returned error",
                    "detail", e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            log.warn("AI analysis via Dapr unavailable ({}): {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "AI service temporarily unavailable",
                    "detail", "processor-atomizers is not reachable via Dapr sidecar"));
        } catch (Exception e) {
            log.error("AI analysis proxy error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "AI proxy error", "detail", e.getMessage()));
        }
    }

    /**
     * AI token quota and cost tracking — proxied to processor-atomizers via Dapr.
     * GET /api/query/ai/quota
     */
    @GetMapping("/ai/quota")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> getAiQuota(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        String url = UriComponentsBuilder
                .fromUriString(daprInvokeUrl(APP_ATOMIZERS, "/api/v1/ai/quota"))
                .queryParamIfPresent("orgId", java.util.Optional.ofNullable(orgId))
                .toUriString();
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("AI quota call returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", "AI quota service returned error",
                    "detail", e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            log.warn("AI quota via Dapr unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "AI quota service temporarily unavailable",
                    "detail", "processor-atomizers is not reachable via Dapr sidecar"));
        } catch (Exception e) {
            log.error("AI quota proxy error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "AI quota proxy error", "detail", e.getMessage()));
        }
    }

    /**
     * MCP server health check — proxied to processor-generators via Dapr.
     * GET /api/query/mcp/health
     */
    @GetMapping("/mcp/health")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> mcpHealth() {

        String url = daprInvokeUrl(APP_GENERATORS, "/api/v1/mcp/health");
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("MCP health check returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "status", "DOWN",
                    "service", "mcp-server",
                    "detail", e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            log.warn("MCP health via Dapr unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "service", "mcp-server",
                    "detail", "processor-generators is not reachable via Dapr sidecar"));
        } catch (Exception e) {
            log.error("MCP health proxy error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "UNKNOWN", "detail", e.getMessage()));
        }
    }

    /** Builds the Dapr HTTP service-invocation URL for the given app and method path. */
    private String daprInvokeUrl(String appId, String methodPath) {
        return "http://" + daprHost + ":" + daprHttpPort
                + "/v1.0/invoke/" + appId + "/method" + methodPath;
    }
}
