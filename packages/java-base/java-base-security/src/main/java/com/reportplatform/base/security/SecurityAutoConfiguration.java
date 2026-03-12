package com.reportplatform.base.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for security components.
 * <p>
 * Activated by setting the following properties:
 * <ul>
 *     <li>{@code reportplatform.security.azure.tenant-id} - Azure Entra ID tenant ID</li>
 *     <li>{@code reportplatform.security.azure.client-id} - Application (client) ID / expected audience</li>
 * </ul>
 * <p>
 * Can be disabled with {@code reportplatform.security.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "reportplatform.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "reportplatform.security.azure", name = {"tenant-id", "client-id"})
    public JwtValidationUtil jwtValidationUtil(
            @Value("${reportplatform.security.azure.tenant-id}") String tenantId,
            @Value("${reportplatform.security.azure.client-id}") String clientId) {
        log.info("Configuring JWT validation for Azure Entra ID tenant: {}", tenantId);
        return new JwtValidationUtil(tenantId, clientId);
    }
}
