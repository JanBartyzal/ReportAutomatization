package com.reportplatform.orch.service;

import com.reportplatform.orch.config.ServiceRoutingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service that determines where to route data for storage based on whether
 * a mapping template has a promoted dedicated table.
 *
 * Queries engine-data (table sink) via Dapr gRPC to check the promoted_tables_registry
 * and returns a routing decision indicating whether to use the promoted table,
 * the JSONB store, or both (dual-write).
 */
@Service
public class DataRoutingService {

    private static final Logger logger = LoggerFactory.getLogger(DataRoutingService.class);

    private final ServiceRoutingConfig routingConfig;

    public DataRoutingService(ServiceRoutingConfig routingConfig) {
        this.routingConfig = routingConfig;
    }

    /**
     * Determine the routing decision for a given mapping template.
     *
     * Checks whether the mapping template has been promoted to a dedicated table
     * and whether the dual-write window is still active.
     *
     * @param mappingTemplateId the mapping template ID to check
     * @return a RoutingDecision indicating where data should be stored
     */
    public RoutingDecision getRoutingDecision(UUID mappingTemplateId) {
        logger.info("Getting routing decision for mapping template: {}", mappingTemplateId);

        // TODO: Call engine-data (table sink) via Dapr gRPC to check promoted_tables_registry
        // Example Dapr invocation:
        // Map<String, String> request = Map.of("mappingTemplateId", mappingTemplateId.toString());
        // Map<String, Object> response = daprClient.invokeMethod(
        //     routingConfig.engineData(),
        //     "checkPromotedTable",
        //     request,
        //     HttpExtension.POST,
        //     Map.class
        // ).block();
        //
        // If response contains a promoted table entry:
        //   - Check if dual_write_until is in the future -> isDualWrite = true
        //   - Extract tableName from response
        //   - Return RoutingDecision(true, tableName, isDualWrite)
        //
        // If no promoted table exists:
        //   - Return RoutingDecision(false, null, false)

        logger.debug("TODO: Query engine-data for promoted table status of mapping template {}",
                mappingTemplateId);

        // Default: no promoted table, use JSONB store
        return new RoutingDecision(false, null, false);
    }

    /**
     * Represents a routing decision for data storage.
     */
    public static class RoutingDecision {

        private final boolean usePromotedTable;
        private final String tableName;
        private final boolean isDualWrite;

        public RoutingDecision(boolean usePromotedTable, String tableName, boolean isDualWrite) {
            this.usePromotedTable = usePromotedTable;
            this.tableName = tableName;
            this.isDualWrite = isDualWrite;
        }

        /**
         * Whether to use the promoted dedicated table for storage.
         */
        public boolean isUsePromotedTable() {
            return usePromotedTable;
        }

        /**
         * The name of the promoted table, or null if no promoted table exists.
         */
        public String getTableName() {
            return tableName;
        }

        /**
         * Whether to write to both the JSONB store and the promoted table
         * (during the dual-write transition period).
         */
        public boolean isDualWrite() {
            return isDualWrite;
        }

        @Override
        public String toString() {
            return "RoutingDecision{" +
                    "usePromotedTable=" + usePromotedTable +
                    ", tableName='" + tableName + '\'' +
                    ", isDualWrite=" + isDualWrite +
                    '}';
        }
    }
}
