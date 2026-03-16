package com.reportplatform.orch;

import com.reportplatform.orch.config.ServiceRoutingConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * MS-ORCH: Custom Orchestrator microservice.
 * <p>
 * Manages file processing workflows using a state machine and saga pattern.
 * Coordinates work across atomizer, template, and sink microservices via Dapr.
 * </p>
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableConfigurationProperties(ServiceRoutingConfig.class)
public class MsOrchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsOrchApplication.class, args);
    }
}
