package com.reportplatform.enginereporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.reportplatform.enginereporting",
        "com.reportplatform.lifecycle",
        "com.reportplatform.period",
        "com.reportplatform.form",
        "com.reportplatform.tmplpptx",
        "com.reportplatform.notif"
})
@EnableScheduling
@EnableAsync
public class EngineReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineReportingApplication.class, args);
    }
}
