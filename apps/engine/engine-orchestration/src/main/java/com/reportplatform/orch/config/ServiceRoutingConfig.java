package com.reportplatform.orch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable Dapr app-id routing for all downstream services.
 * <p>
 * Maps logical service groups to their Dapr app-ids, allowing routing
 * changes without code modifications. After the P8 consolidation,
 * multiple old microservices are merged into consolidated services.
 * </p>
 *
 * <pre>
 * Consolidation mapping:
 *   engine-core       <- ms-auth, ms-admin, ms-batch, ms-ver, ms-audit
 *   engine-data       <- ms-sink-tbl, ms-sink-doc, ms-sink-log, ms-tmpl, ms-qry, ms-dash, ms-srch
 *   engine-reporting   <- ms-lifecycle, ms-period, ms-form, ms-tmpl-pptx, ms-notif
 *   processor-atomizers <- ms-atm-pptx, ms-atm-xls, ms-atm-csv, ms-atm-pdf, ms-atm-ai, ms-atm-cln
 *   processor-generators <- ms-gen-pptx, ms-gen-xls, ms-mcp
 *   engine-integrations  <- ms-ext-snow
 *   engine-ingestor      <- ms-ing
 * </pre>
 */
@ConfigurationProperties(prefix = "routing")
public record ServiceRoutingConfig(
        String engineCore,
        String engineData,
        String engineReporting,
        String processorAtomizers,
        String processorGenerators,
        String engineIntegrations,
        String engineIngestor
) {}
