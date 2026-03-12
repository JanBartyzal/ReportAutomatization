package com.reportplatform.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MS-ADMIN - Administration Service
 * 
 * Provides endpoints for:
 * - Organization hierarchy management (Holding → Company → Division)
 * - User and role management
 * - API key lifecycle management
 * - Failed job reprocessing (DLQ viewer)
 */
@SpringBootApplication
public class MsAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAdminApplication.class, args);
    }
}
