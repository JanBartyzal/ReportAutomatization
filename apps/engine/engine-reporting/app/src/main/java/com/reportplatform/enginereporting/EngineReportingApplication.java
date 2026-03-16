package com.reportplatform.enginereporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {
        "com.reportplatform.enginereporting",
        "com.reportplatform.lifecycle",
        "com.reportplatform.period",
        "com.reportplatform.form",
        "com.reportplatform.tmplpptx",
        "com.reportplatform.notif"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableJpaRepositories(basePackages = {
    "com.reportplatform.enginereporting",
    "com.reportplatform.lifecycle",
    "com.reportplatform.period",
    "com.reportplatform.form",
    "com.reportplatform.tmplpptx",
    "com.reportplatform.notif"
})
@EntityScan(basePackages = "com.reportplatform")
@EnableScheduling
@EnableAsync
public class EngineReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineReportingApplication.class, args);
    }
}
