package com.reportplatform.snow.service;

import com.reportplatform.snow.model.entity.AuthType;
import com.reportplatform.snow.model.dto.ServiceNowTableDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ServiceNowClient {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNowClient.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final WebClient.Builder webClientBuilder;

    public ServiceNowClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Authenticate against a ServiceNow instance.
     * For BASIC auth, credentialsRef is expected to be "username:password" (retrieved from KeyVault).
     * For OAUTH2, credentialsRef is expected to be "clientId:clientSecret" (retrieved from KeyVault).
     *
     * @return access token (for OAUTH2) or Base64-encoded credentials (for BASIC)
     */
    public String authenticate(String instanceUrl, AuthType authType, String credentialsRef) {
        logger.info("Authenticating to ServiceNow instance: {} with auth type: {}", instanceUrl, authType);

        if (authType == AuthType.BASIC) {
            // For Basic Auth, encode credentials as Base64
            String encoded = Base64.getEncoder().encodeToString(credentialsRef.getBytes());
            logger.debug("Basic auth credentials encoded for instance: {}", instanceUrl);
            return encoded;
        }

        // OAUTH2 flow
        String[] parts = credentialsRef.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("OAuth2 credentials must be in format 'clientId:clientSecret'");
        }

        String clientId = parts[0];
        String clientSecret = parts[1];

        WebClient client = webClientBuilder.baseUrl(instanceUrl).build();

        Map<String, Object> tokenResponse = client.post()
                .uri("/oauth_token.do")
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(30));

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain OAuth2 token from ServiceNow instance: " + instanceUrl);
        }

        String accessToken = (String) tokenResponse.get("access_token");
        logger.info("Successfully obtained OAuth2 token for instance: {}", instanceUrl);
        return accessToken;
    }

    /**
     * Fetch records from a ServiceNow table with pagination support.
     *
     * @param instanceUrl         the SN instance base URL
     * @param tableName           the table to query
     * @param accessToken         the auth token (Basic encoded or OAuth2 bearer)
     * @param offset              pagination offset (sysparm_offset)
     * @param limit               page size (sysparm_limit)
     * @param sysUpdatedOnAfter   optional filter for incremental sync (sys_updated_on > value)
     * @return list of table data DTOs
     */
    /** Backward-compatible overload (generic sync without field selection). */
    public List<ServiceNowTableDataDTO> fetchTable(String instanceUrl, String tableName,
                                                    String accessToken, int offset, int limit,
                                                    String sysUpdatedOnAfter) {
        return fetchTable(instanceUrl, tableName, accessToken, offset, limit,
                sysUpdatedOnAfter, null, null);
    }

    /**
     * Fetch records from a ServiceNow table with field selection and custom query support.
     *
     * @param sysUpdatedOnAfter optional incremental filter (sys_updated_on > value)
     * @param sysparmFields     comma-separated list of fields to return (null = all)
     * @param customQuery       full sysparm_query string overriding the default filter
     */
    public List<ServiceNowTableDataDTO> fetchTable(String instanceUrl, String tableName,
                                                    String accessToken, int offset, int limit,
                                                    String sysUpdatedOnAfter,
                                                    String sysparmFields,
                                                    String customQuery) {
        logger.info("Fetching table '{}' from {} (offset={}, limit={}, query={})",
                tableName, instanceUrl, offset, limit, customQuery);

        String query = customQuery != null && !customQuery.isBlank() ? customQuery : "";
        if (query.isBlank() && sysUpdatedOnAfter != null && !sysUpdatedOnAfter.isBlank()) {
            query = "sys_updated_on>" + sysUpdatedOnAfter;
        }

        String finalQuery = query;
        String finalFields = sysparmFields;
        return executeWithRetry(() ->
                doFetchTable(instanceUrl, tableName, accessToken, offset, limit, finalQuery, finalFields));
    }

    /**
     * Test connectivity to a ServiceNow instance.
     *
     * @return a map with instance info (e.g., build version) or throws on failure
     */
    public Map<String, Object> testConnection(String instanceUrl, String accessToken) {
        logger.info("Testing connection to ServiceNow instance: {}", instanceUrl);

        WebClient client = webClientBuilder.baseUrl(instanceUrl).build();

        Map<String, Object> response = client.get()
                .uri("/api/now/table/sys_properties?sysparm_query=name=glide.buildtag&sysparm_limit=1")
                .headers(headers -> applyAuthHeader(headers, accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(15));

        logger.info("Connection test successful for instance: {}", instanceUrl);
        return response;
    }

    // ==================== Private Helpers ====================

    @SuppressWarnings("unchecked")
    private List<ServiceNowTableDataDTO> doFetchTable(String instanceUrl, String tableName,
                                                       String accessToken, int offset, int limit,
                                                       String query) {
        return doFetchTable(instanceUrl, tableName, accessToken, offset, limit, query, null);
    }

    @SuppressWarnings("unchecked")
    private List<ServiceNowTableDataDTO> doFetchTable(String instanceUrl, String tableName,
                                                       String accessToken, int offset, int limit,
                                                       String query, String fields) {
        WebClient client = webClientBuilder.baseUrl(instanceUrl).build();

        Map<String, Object> response = client.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/now/table/{tableName}")
                            .queryParam("sysparm_offset", offset)
                            .queryParam("sysparm_limit", limit);
                    if (query != null && !query.isBlank()) {
                        uriBuilder.queryParam("sysparm_query", query);
                    }
                    if (fields != null && !fields.isBlank()) {
                        uriBuilder.queryParam("sysparm_fields", fields);
                    }
                    return uriBuilder.build(tableName);
                })
                .headers(headers -> applyAuthHeader(headers, accessToken))
                .exchangeToMono(clientResponse -> {
                    // Handle rate limiting
                    handleRateLimitHeaders(clientResponse.headers().asHttpHeaders());

                    HttpStatusCode status = clientResponse.statusCode();
                    if (status.is2xxSuccessful()) {
                        return clientResponse.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
                    }

                    if (status.value() == 429 || status.value() == 503) {
                        return clientResponse.createException().flatMap(Mono::error);
                    }

                    return clientResponse.createException().flatMap(Mono::error);
                })
                .block(Duration.ofSeconds(60));

        if (response == null || !response.containsKey("result")) {
            logger.warn("No results returned from table '{}' at offset {}", tableName, offset);
            return List.of();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");
        return results.stream()
                .map(record -> {
                    ServiceNowTableDataDTO dto = new ServiceNowTableDataDTO();
                    dto.setSysId(record.containsKey("sys_id") ? String.valueOf(record.get("sys_id")) : null);
                    dto.setFields(record);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void applyAuthHeader(HttpHeaders headers, String accessToken) {
        // If the token looks like Base64-encoded Basic credentials (contains no dots/spaces typical of JWT)
        // we treat it as Basic auth; otherwise as Bearer token
        if (accessToken != null && !accessToken.contains(".")) {
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
        } else {
            headers.setBearerAuth(accessToken);
        }
    }

    private void handleRateLimitHeaders(HttpHeaders headers) {
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        String resetHeader = headers.getFirst("X-RateLimit-Reset");

        if (remaining != null) {
            int remainingCount = Integer.parseInt(remaining);
            if (remainingCount == 0 && resetHeader != null) {
                long resetEpoch = Long.parseLong(resetHeader);
                long nowEpoch = System.currentTimeMillis() / 1000;
                long waitSeconds = Math.max(1, resetEpoch - nowEpoch);
                logger.warn("Rate limit exhausted. Waiting {} seconds until reset.", waitSeconds);
                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for rate limit reset", e);
                }
            }
        }
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            try {
                return operation.execute();
            } catch (WebClientResponseException ex) {
                int statusCode = ex.getStatusCode().value();
                if ((statusCode == 429 || statusCode == 503) && attempt < MAX_RETRIES) {
                    attempt++;
                    logger.warn("Received HTTP {} from ServiceNow. Retry attempt {}/{} after {}ms backoff.",
                            statusCode, attempt, MAX_RETRIES, backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw ex;
                }
            } catch (Exception ex) {
                if (attempt < MAX_RETRIES && isRetryable(ex)) {
                    attempt++;
                    logger.warn("Retryable error from ServiceNow. Retry attempt {}/{} after {}ms backoff.",
                            attempt, MAX_RETRIES, backoffMs, ex);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw ex;
                }
            }
        }
    }

    private boolean isRetryable(Exception ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof WebClientResponseException) {
            int code = ((WebClientResponseException) cause).getStatusCode().value();
            return code == 429 || code == 503;
        }
        return false;
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute();
    }
}
