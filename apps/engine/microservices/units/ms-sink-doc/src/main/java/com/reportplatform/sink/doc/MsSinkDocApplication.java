package com.reportplatform.sink.doc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for MS-SINK-DOC (Document Sink Service).
 * 
 * This service handles write operations for document storage:
 * - Store document content
 * - Publish events for async embedding generation
 * - Delete operations for Saga compensation
 * 
 * Communication: gRPC via Dapr sidecar (no REST endpoints)
 */
@SpringBootApplication
public class MsSinkDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSinkDocApplication.class, args);
    }
}
