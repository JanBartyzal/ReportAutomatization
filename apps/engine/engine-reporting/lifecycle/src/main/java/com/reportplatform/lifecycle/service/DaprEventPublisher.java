package com.reportplatform.lifecycle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class DaprEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DaprEventPublisher.class);

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    private final ObjectMapper objectMapper;
    private DaprClient daprClient;

    public DaprEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        this.daprClient = new DaprClientBuilder().build();
    }

    @PreDestroy
    void destroy() throws Exception {
        if (daprClient != null) {
            daprClient.close();
        }
    }

    public void publishStatusChanged(UUID reportId, String orgId, String fromStatus,
            String toStatus, String userId) {
        Map<String, Object> event = Map.of(
                "reportId", reportId.toString(),
                "orgId", orgId,
                "fromStatus", fromStatus != null ? fromStatus : "",
                "toStatus", toStatus,
                "userId", userId,
                "timestamp", System.currentTimeMillis());

        try {
            daprClient.publishEvent(pubsubName, "report.status_changed", event).block();
            log.info("Published report.status_changed: reportId={}, {} -> {}", reportId, fromStatus, toStatus);
        } catch (Exception e) {
            log.error("Failed to publish report.status_changed for reportId={}: {}", reportId, e.getMessage(), e);
        }
    }

    public void publishLocalReleased(UUID reportId, String orgId, String userId) {
        Map<String, Object> event = Map.of(
                "reportId", reportId.toString(),
                "orgId", orgId,
                "userId", userId,
                "timestamp", System.currentTimeMillis());

        try {
            daprClient.publishEvent(pubsubName, "report.local_released", event).block();
            log.info("Published report.local_released: reportId={}", reportId);
        } catch (Exception e) {
            log.error("Failed to publish report.local_released for reportId={}: {}", reportId, e.getMessage(), e);
        }
    }

    public void publishDataLocked(UUID reportId, String orgId, String userId) {
        Map<String, Object> event = Map.of(
                "reportId", reportId.toString(),
                "orgId", orgId,
                "userId", userId,
                "timestamp", System.currentTimeMillis());

        try {
            daprClient.publishEvent(pubsubName, "report.data_locked", event).block();
            log.info("Published report.data_locked: reportId={}", reportId);
        } catch (Exception e) {
            log.error("Failed to publish report.data_locked for reportId={}: {}", reportId, e.getMessage(), e);
        }
    }
}
