package com.reportplatform.engineintegrations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.reportplatform.engineintegrations",
    "com.reportplatform.snow"
})
@EnableScheduling
public class EngineIntegrationsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineIntegrationsApplication.class, args);
    }
}
