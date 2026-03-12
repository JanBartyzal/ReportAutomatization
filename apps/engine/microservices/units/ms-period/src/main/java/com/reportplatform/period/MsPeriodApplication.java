package com.reportplatform.period;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsPeriodApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPeriodApplication.class, args);
    }
}
