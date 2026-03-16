package com.reportplatform.orch.service;

import com.reportplatform.orch.config.ServiceRoutingConfig;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
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
    private final DaprClient daprClient;

    public DataRoutingService(ServiceRoutingConfig routingConfig, DaprClient daprClient) {
        this.routingConfig = routingConfig;
        this.daprClient = daprClient;
    }

    /**
     * Determine the routing decision for a given mapping template.
     *
     * Calls engine-data's GetRoutingInfo method via Dapr to check whether the
     * mapping template has been promoted to a dedicated table.
     *
     * @param mappingTemplateId the mapping template ID to check
     * @return a RoutingDecision indicating where data should be stored
     */
    @SuppressWarnings("unchecked")
    public RoutingDecision getRoutingDecision(UUID mappingTemplateId) {
        if (mappingTemplateId == null) {
            logger.debug("No mapping template ID provided, using default JSONB store");
            return new RoutingDecision(false, null, false);
        }

        logger.info("Getting routing decision for mapping template: {}", mappingTemplateId);

        try {
            Map<String, String> request = Map.of("mappingTemplateId", mappingTemplateId.toString());

            Map<String, Object> response = daprClient.invokeMethod(
                    routingConfig.engineData(),
                    "getRoutingInfo",
                    request,
                    HttpExtension.POST,
                    Map.class
            ).block();

            if (response == null) {
                logger.warn("Null response from engine-data getRoutingInfo, using default JSONB store");
                return new RoutingDecision(false, null, false);
            }

            boolean hasPromotedTable = Boolean.TRUE.equals(response.get("hasPromotedTable"));
            String tableName = (String) response.get("tableName");
            boolean inDualWrite = Boolean.TRUE.equals(response.get("inDualWritePeriod"));

            logger.info("Routing decision for template {}: promoted={}, table={}, dualWrite={}",
                    mappingTemplateId, hasPromotedTable, tableName, inDualWrite);

            return new RoutingDecision(hasPromotedTable, tableName, inDualWrite);

        } catch (Exception e) {
            logger.warn("Failed to get routing info for template {}, falling back to JSONB: {}",
                    mappingTemplateId, e.getMessage());
            return new RoutingDecision(false, null, false);
        }
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
