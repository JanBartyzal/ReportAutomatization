package com.reportplatform.sink.tbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for MS-SINK-TBL (Table Sink Service).
 * 
 * This service handles write operations for structured table data:
 * - Bulk insert of parsed table data
 * - Form response storage
 * - Delete operations for Saga compensation
 * 
 * Communication: gRPC via Dapr sidecar (no REST endpoints)
 */
@SpringBootApplication
public class MsSinkTblApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSinkTblApplication.class, args);
    }
}
