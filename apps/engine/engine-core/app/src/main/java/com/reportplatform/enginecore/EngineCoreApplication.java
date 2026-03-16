package com.reportplatform.enginecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = {
        "com.reportplatform.enginecore",
        "com.reportplatform.auth",
        "com.reportplatform.admin",
        "com.reportplatform.batch",
        "com.reportplatform.ver",
        "com.reportplatform.audit"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableJpaRepositories(basePackages = {
    "com.reportplatform.enginecore",
    "com.reportplatform.auth",
    "com.reportplatform.admin",
    "com.reportplatform.batch",
    "com.reportplatform.ver",
    "com.reportplatform.audit"
})
@EntityScan(basePackages = "com.reportplatform")
public class EngineCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineCoreApplication.class, args);
    }
}
