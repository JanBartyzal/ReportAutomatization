package com.reportplatform.enginedata.common.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Dapr configuration for all engine-data modules.
 * Provides a singleton DaprClient bean for pub/sub and service invocation.
 */
@Configuration
public class DaprConfig {

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    @Bean
    public DaprClient daprClient() {
        return new DaprClientBuilder().build();
    }

    public String getPubsubName() {
        return pubsubName;
    }
}
