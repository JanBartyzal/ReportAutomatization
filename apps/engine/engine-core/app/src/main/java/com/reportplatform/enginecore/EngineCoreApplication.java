package com.reportplatform.enginecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.reportplatform.enginecore",
    "com.reportplatform.auth",
    "com.reportplatform.admin",
    "com.reportplatform.batch",
    "com.reportplatform.ver",
    "com.reportplatform.audit"
})
public class EngineCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineCoreApplication.class, args);
    }
}
