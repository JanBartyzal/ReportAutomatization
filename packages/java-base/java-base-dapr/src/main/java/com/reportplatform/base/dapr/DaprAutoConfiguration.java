package com.reportplatform.base.dapr;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the Dapr client wrapper.
 * <p>
 * Automatically creates a {@link DaprClient} and wraps it with
 * {@link DaprClientWrapper} when the Dapr SDK is on the classpath.
 * <p>
 * Can be disabled with {@code reportplatform.dapr.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass(DaprClient.class)
@ConditionalOnProperty(
        prefix = "reportplatform.dapr",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DaprAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DaprAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public DaprClient daprClient() {
        log.info("Creating Dapr client");
        return new DaprClientBuilder().build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DaprClientWrapper daprClientWrapper(DaprClient daprClient) {
        log.info("Creating DaprClientWrapper with exponential backoff retry policy "
                + "(3 retries: 1s/5s/30s)");
        return new DaprClientWrapper(daprClient);
    }
}
