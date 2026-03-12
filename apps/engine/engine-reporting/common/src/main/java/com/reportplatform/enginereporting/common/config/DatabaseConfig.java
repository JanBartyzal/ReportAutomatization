package com.reportplatform.enginereporting.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Unified database configuration for the consolidated reporting service.
 * All modules share a single datasource (engine_reporting_db) with Hikari connection pool.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
        "com.reportplatform.lifecycle.repository",
        "com.reportplatform.period.repository",
        "com.reportplatform.form.repository",
        "com.reportplatform.tmplpptx.repository",
        "com.reportplatform.notif.repository"
})
public class DatabaseConfig {
    // All database configuration is provided via application.yml
    // This class enables JPA repositories scanning across all module packages
}
