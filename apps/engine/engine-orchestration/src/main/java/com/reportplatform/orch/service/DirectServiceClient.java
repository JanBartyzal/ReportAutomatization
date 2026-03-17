package com.reportplatform.orch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Wraps Dapr service invocation with a direct HTTP fallback.
 * <p>
 * When the Dapr sidecar is unreachable (no sidecar in Docker Compose),
 * falls back to calling the target service directly via HTTP using
 * the Docker Compose service names.
 * </p>
 */
@Component
public class DirectServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DirectServiceClient.class);

    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Maps Dapr app-ids to their direct HTTP URLs.
     * In Docker Compose, services are accessible by container name.
     */
    private final Map<String, String> serviceUrls;

    public DirectServiceClient(
            DaprClient daprClient,
            ObjectMapper objectMapper,
            @Value("${direct.processor-atomizers:http://processor-atomizers:8088}") String atomizerUrl,
            @Value("${direct.engine-data:http://engine-data:8100}") String dataUrl,
            @Value("${direct.engine-core:http://engine-core:8081}") String coreUrl) {
        this.daprClient = daprClient;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.serviceUrls = Map.of(
                "processor-atomizers", atomizerUrl,
                "engine-data", dataUrl,
                "engine-core", coreUrl
        );
    }

    /**
     * Invoke a method on a service. Tries Dapr first, falls back to direct HTTP.
     *
     * @param appId     Dapr app-id (also used to resolve direct URL)
     * @param method    Method/path to invoke
     * @param request   Request payload
     * @param responseType Response class
     * @return response from the service
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeMethod(String appId, String method, Object request, Class<T> responseType) {
        try {
            T result = (T) daprClient.invokeMethod(appId, method, request,
                    HttpExtension.POST, responseType).block();
            log.debug("Dapr invocation succeeded: {}/{}", appId, method);
            return result;
        } catch (Throwable e) {
            log.warn("Dapr invocation failed for {}/{}, falling back to direct HTTP: {}",
                    appId, method, e.getMessage());
            return invokeDirectHttp(appId, method, request, responseType);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeDirectHttp(String appId, String method, Object request, Class<T> responseType) {
        String baseUrl = serviceUrls.getOrDefault(appId, "http://" + appId + ":8080");
        // Map Dapr method names to REST API paths
        String path = mapMethodToPath(method);
        String url = baseUrl + path;

        try {
            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Direct HTTP call succeeded: {} -> {}", url, response.statusCode());
                if (responseType == Map.class) {
                    return (T) objectMapper.readValue(response.body(), Map.class);
                }
                return objectMapper.readValue(response.body(), responseType);
            } else {
                throw new RuntimeException("Direct HTTP call to " + url + " failed with status " +
                        response.statusCode() + ": " + response.body());
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new RuntimeException("Direct HTTP call to " + url + " failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Maps Dapr method names to REST API paths on the target service.
     */
    private String mapMethodToPath(String method) {
        return switch (method) {
            case "scan" -> "/api/v1/scan";
            case "parse" -> "/api/v1/parse";
            case "map" -> "/api/v1/map";
            case "store" -> "/api/v1/store";
            case "store-doc" -> "/api/v1/store-doc";
            case "rollback" -> "/api/v1/rollback";
            case "rollback-doc" -> "/api/v1/rollback-doc";
            default -> {
                if (method.startsWith("/")) {
                    yield method;
                }
                yield "/api/v1/" + method;
            }
        };
    }
}
