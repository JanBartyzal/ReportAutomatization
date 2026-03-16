package com.reportplatform.enginereporting.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Unified database configuration for the consolidated reporting service.
 * JPA repositories are scanned via @EnableJpaRepositories on the main application class.
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    // All database configuration is provided via application.yml
    // This class enables JPA repositories scanning across all module packages
}
