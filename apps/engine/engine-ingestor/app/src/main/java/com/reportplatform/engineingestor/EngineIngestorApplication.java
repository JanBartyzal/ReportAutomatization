package com.reportplatform.engineingestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = {
        "com.reportplatform.engineingestor",
        "com.reportplatform.ing",
        "com.reportplatform.scan"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableJpaRepositories(basePackages = {
    "com.reportplatform.engineingestor",
    "com.reportplatform.ing",
    "com.reportplatform.scan"
})
@EntityScan(basePackages = "com.reportplatform")
public class EngineIngestorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineIngestorApplication.class, args);
    }
}
