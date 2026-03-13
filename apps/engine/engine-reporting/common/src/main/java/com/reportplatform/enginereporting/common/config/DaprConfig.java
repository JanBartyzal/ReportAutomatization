package com.reportplatform.enginereporting.common.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized Dapr client configuration for the consolidated reporting service.
 * Provides a single shared DaprClient bean instead of each module creating its
 * own.
 */
@Configuration
public class DaprConfig {

    private static final Logger log = LoggerFactory.getLogger(DaprConfig.class);

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    @Value("${dapr.statestore.name:reportplatform-statestore}")
    private String statestoreName;

    private DaprClient daprClient;

    @Bean
    public DaprClient daprClient() {
        this.daprClient = new DaprClientBuilder().build();
        log.info("Initialized shared DaprClient for engine-reporting (pubsub={}, statestore={})",
                pubsubName, statestoreName);
        return this.daprClient;
    }

    @PreDestroy
    void destroy() throws Exception {
        if (daprClient != null) {
            daprClient.close();
            log.info("Closed shared DaprClient");
        }
    }

    public String getPubsubName() {
        return pubsubName;
    }

    public String getStatestoreName() {
        return statestoreName;
    }
}
