package com.reportplatform.notif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MS-NOTIF - Notification Service
 * Handles in-app notifications and real-time push for lifecycle events.
 */
@SpringBootApplication
@EnableScheduling
public class MsNotifApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNotifApplication.class, args);
    }
}
