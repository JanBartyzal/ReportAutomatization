package com.reportplatform.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MS-BATCH - Batch Management Service
 * 
 * Provides endpoints for:
 * - Batch lifecycle management (OPEN, COLLECTING, CLOSED)
 * - Organization metadata for files
 * - Period-based aggregation queries
 */
@SpringBootApplication
public class MsBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsBatchApplication.class, args);
    }
}
