package com.reportplatform.period.service;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DaprEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DaprEventPublisher.class);

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    private DaprClient daprClient;

    @PostConstruct
    void init() {
        this.daprClient = new DaprClientBuilder().build();
    }

    @PreDestroy
    void destroy() {
        if (daprClient != null) {
            daprClient.close();
        }
    }

    public void publishDeadlineReminder(UUID periodId, String periodName, int daysRemaining,
                                        List<String> orgIds) {
        Map<String, Object> event = Map.of(
                "type", "deadline_reminder",
                "periodId", periodId.toString(),
                "periodName", periodName,
                "daysRemaining", daysRemaining,
                "orgIds", orgIds,
                "timestamp", System.currentTimeMillis()
        );

        try {
            daprClient.publishEvent(pubsubName, "notify", event).block();
            log.info("Published deadline reminder: period={}, days={}, orgs={}",
                    periodId, daysRemaining, orgIds.size());
        } catch (Exception e) {
            log.error("Failed to publish deadline reminder for period={}: {}",
                    periodId, e.getMessage(), e);
        }
    }

    public void publishDeadlineEscalation(UUID periodId, String periodName,
                                          List<String> nonCompliantOrgIds) {
        Map<String, Object> event = Map.of(
                "type", "deadline_escalation",
                "periodId", periodId.toString(),
                "periodName", periodName,
                "nonCompliantOrgIds", nonCompliantOrgIds,
                "timestamp", System.currentTimeMillis()
        );

        try {
            daprClient.publishEvent(pubsubName, "notify", event).block();
            log.info("Published deadline escalation: period={}, nonCompliant={}",
                    periodId, nonCompliantOrgIds.size());
        } catch (Exception e) {
            log.error("Failed to publish deadline escalation for period={}: {}",
                    periodId, e.getMessage(), e);
        }
    }
}
