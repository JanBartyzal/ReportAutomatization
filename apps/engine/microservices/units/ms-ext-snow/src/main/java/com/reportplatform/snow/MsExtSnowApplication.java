package com.reportplatform.snow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MS-EXT-SNOW - Service-Now Integration Service
 *
 * Provides endpoints for:
 * - Service-Now connection management (OAUTH2 / BASIC auth)
 * - Scheduled data synchronization from Service-Now tables
 * - Report distribution to Service-Now
 * - Connection health testing
 */
@SpringBootApplication
@EnableScheduling
public class MsExtSnowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsExtSnowApplication.class, args);
    }
}
