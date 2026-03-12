package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    public void init() {
        try {
            this.daprClient = new DaprClientBuilder().build();
            log.info("Dapr client initialized for MS-VER");
        } catch (Exception e) {
            log.warn("Failed to initialize Dapr client - events will not be published", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (daprClient != null) {
            try {
                daprClient.close();
            } catch (Exception e) {
                log.warn("Error closing Dapr client", e);
            }
        }
    }

    public void publishVersionCreated(UUID versionId, String entityType, UUID entityId,
                                       int versionNumber, UUID orgId) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("versionId", versionId.toString());
        event.put("entityType", entityType);
        event.put("entityId", entityId.toString());
        event.put("versionNumber", versionNumber);
        event.put("orgId", orgId.toString());

        publish("version.created", event);
    }

    public void publishEditOnLocked(String entityType, UUID entityId,
                                     int versionNumber, UUID orgId) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("entityType", entityType);
        event.put("entityId", entityId.toString());
        event.put("versionNumber", versionNumber);
        event.put("orgId", orgId.toString());

        publish("version.edit_on_locked", event);
    }

    private void publish(String topic, Object data) {
        if (daprClient == null) {
            log.warn("Dapr client not available, skipping publish to {}", topic);
            return;
        }
        try {
            daprClient.publishEvent(pubsubName, topic, data).block();
            log.debug("Published event to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {}", topic, e);
        }
    }
}
