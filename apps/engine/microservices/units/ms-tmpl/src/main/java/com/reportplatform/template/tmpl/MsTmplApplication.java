package com.reportplatform.template.tmpl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for MS-TMPL (Template & Schema Mapping Registry).
 *
 * This service handles schema mapping operations:
 * - Apply mapping rules to extracted data before sink write
 * - Suggest mappings based on column headers (AI-assisted + history)
 * - Map Excel columns to form fields (FS19 prep)
 *
 * Communication: gRPC via Dapr sidecar (called from MS-ORCH)
 */
@SpringBootApplication
public class MsTmplApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsTmplApplication.class, args);
    }
}
