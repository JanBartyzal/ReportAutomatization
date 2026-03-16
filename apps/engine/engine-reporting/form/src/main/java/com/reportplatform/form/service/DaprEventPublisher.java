package com.reportplatform.form.service;

import com.reportplatform.form.model.FormResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("formDaprEventPublisher")
public class DaprEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DaprEventPublisher.class);

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    private final com.reportplatform.base.dapr.DaprClientWrapper daprClient;

    public DaprEventPublisher(com.reportplatform.base.dapr.DaprClientWrapper daprClient) {
        this.daprClient = daprClient;
    }

    public void publishFormResponseSubmitted(FormResponseEntity response) {
        var eventData = Map.of(
                "responseId", response.getId().toString(),
                "formId", response.getFormId().toString(),
                "formVersionId", response.getFormVersionId().toString(),
                "orgId", response.getOrgId(),
                "userId", response.getUserId(),
                "periodId", response.getPeriodId() != null ? response.getPeriodId().toString() : "",
                "submittedAt", response.getSubmittedAt().toString()
        );

        try {
            daprClient.publishEvent(pubsubName, "form.response.submitted", eventData);
            log.info("Published form.response.submitted event for response {}", response.getId());
        } catch (Exception e) {
            log.error("Failed to publish form.response.submitted event for response {}", response.getId(), e);
        }
    }
}
