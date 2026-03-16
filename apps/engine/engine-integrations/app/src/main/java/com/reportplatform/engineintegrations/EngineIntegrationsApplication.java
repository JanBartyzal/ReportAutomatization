package com.reportplatform.engineintegrations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {
        "com.reportplatform.engineintegrations",
        "com.reportplatform.snow"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableJpaRepositories(basePackages = {
    "com.reportplatform.engineintegrations",
    "com.reportplatform.snow"
})
@EntityScan(basePackages = "com.reportplatform")
@EnableScheduling
public class EngineIntegrationsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineIntegrationsApplication.class, args);
    }
}
