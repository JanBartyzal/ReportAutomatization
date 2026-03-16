package com.reportplatform.enginedata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for the consolidated engine-data service.
 * Assembles all data-related modules: sink-tbl, sink-doc, sink-log,
 * query, dashboard, search, and template.
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.reportplatform.enginedata",
        "com.reportplatform.sink",
        "com.reportplatform.qry",
        "com.reportplatform.dash",
        "com.reportplatform.srch",
        "com.reportplatform.template"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableJpaRepositories(basePackages = {
    "com.reportplatform.enginedata",
    "com.reportplatform.sink",
    "com.reportplatform.qry",
    "com.reportplatform.dash",
    "com.reportplatform.srch",
    "com.reportplatform.template"
})
@EntityScan(basePackages = "com.reportplatform")
public class EngineDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineDataApplication.class, args);
    }
}
