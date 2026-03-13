package com.reportplatform.engineingestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.reportplatform.engineingestor",
    "com.reportplatform.ing",
    "com.reportplatform.scan"
})
public class EngineIngestorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineIngestorApplication.class, args);
    }
}
