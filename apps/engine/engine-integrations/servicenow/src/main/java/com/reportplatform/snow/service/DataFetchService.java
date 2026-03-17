package com.reportplatform.snow.service;

import com.reportplatform.base.dapr.DaprClientWrapper;
import com.reportplatform.snow.model.dto.ServiceNowTableDataDTO;
import com.reportplatform.snow.model.entity.AuthType;
import com.reportplatform.snow.model.entity.ServiceNowConnectionEntity;
import com.reportplatform.snow.repository.ServiceNowConnectionRepository;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataFetchService {

    private static final Logger logger = LoggerFactory.getLogger(DataFetchService.class);

    private static final int PAGE_SIZE = 100;

    private final ServiceNowConnectionRepository connectionRepository;
    private final ServiceNowClient serviceNowClient;
    private final KeyVaultService keyVaultService;
    private final DaprClientWrapper daprClientWrapper;

    @Value("${dapr.remote.ms-tmpl-app-id:ms-tmpl}")
    private String msTmplAppId;

    @Value("${dapr.remote.ms-sink-tbl-app-id:ms-sink-tbl}")
    private String msSinkTblAppId;

    public DataFetchService(ServiceNowConnectionRepository connectionRepository,
                            ServiceNowClient serviceNowClient,
                            KeyVaultService keyVaultService,
                            DaprClientWrapper daprClientWrapper) {
        this.connectionRepository = connectionRepository;
        this.serviceNowClient = serviceNowClient;
        this.keyVaultService = keyVaultService;
        this.daprClientWrapper = daprClientWrapper;
    }

    /**
     * Fetch data from ServiceNow and store it via downstream services.
     *
     * @param connectionId        the connection configuration ID
     * @param sysUpdatedOnAfter   optional incremental sync filter (ISO timestamp)
     * @return fetch result with counts
     */
    @Transactional
    public FetchResult fetchAndStore(UUID connectionId, String sysUpdatedOnAfter) {
        logger.info("Starting data fetch for connection: {}", connectionId);

        // Step 1: Load connection config from DB
        ServiceNowConnectionEntity connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        if (!connection.isEnabled()) {
            throw new IllegalStateException("Connection is disabled: " + connectionId);
        }

        // Step 2: Get credentials from KeyVault
        String credentials = keyVaultService.getSecret(connection.getCredentialsRef());

        // Step 3: Authenticate via ServiceNowClient
        AuthType authType = connection.getAuthType();
        String accessToken = serviceNowClient.authenticate(
                connection.getInstanceUrl(), authType, credentials);

        // Step 4: Fetch data from SN tables with pagination
        int totalFetched = 0;
        int totalStored = 0;

        List<String> tables = parseJsonArray(connection.getTables());

        if (tables.isEmpty()) {
            throw new IllegalStateException("No tables configured for connection: " + connectionId);
        }

        List<ServiceNowTableDataDTO> allRecords = new ArrayList<>();

        for (String tableName : tables) {
            int offset = 0;
            logger.info("Fetching table '{}' for connection: {}", tableName, connectionId);

            while (true) {
                List<ServiceNowTableDataDTO> page = serviceNowClient.fetchTable(
                        connection.getInstanceUrl(),
                        tableName,
                        accessToken,
                        offset,
                        PAGE_SIZE,
                        sysUpdatedOnAfter);

                if (page.isEmpty()) {
                    logger.debug("No more records at offset {} for table '{}'. Pagination complete.", offset, tableName);
                    break;
                }

                allRecords.addAll(page);
                totalFetched += page.size();
                offset += page.size();

                logger.debug("Fetched {} records from table '{}' (total so far: {})", page.size(), tableName, totalFetched);

                if (page.size() < PAGE_SIZE) {
                    // Last page for this table
                    break;
                }
            }
        }

        // Step 5a: Send fetched data to MS-TMPL via Dapr for template matching/transformation
        Map<String, Object> transformRequest = Map.of(
                "connectionId", connectionId.toString(),
                "records", allRecords);

        @SuppressWarnings("unchecked")
        Map<String, Object> transformResponse = daprClientWrapper.invokeMethod(
                msTmplAppId,
                "/api/v1/transform",
                transformRequest,
                HttpExtension.POST,
                new TypeRef<Map<String, Object>>() {})
                .block();

        List<Map<String, Object>> transformedRecords = Collections.emptyList();
        if (transformResponse != null && transformResponse.get("transformedRecords") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) transformResponse.get("transformedRecords");
            transformedRecords = records;
        }

        // Step 5b: Send transformed data to MS-SINK-TBL via Dapr for persistence
        if (!transformedRecords.isEmpty()) {
            Map<String, Object> persistRequest = Map.of(
                    "connectionId", connectionId.toString(),
                    "records", transformedRecords);

            Map<String, Object> persistResponse = daprClientWrapper.invokeMethod(
                    msSinkTblAppId,
                    "/api/v1/persist",
                    persistRequest,
                    HttpExtension.POST,
                    new TypeRef<Map<String, Object>>() {})
                    .block();

            if (persistResponse != null && persistResponse.get("recordsPersisted") != null) {
                totalStored = ((Number) persistResponse.get("recordsPersisted")).intValue();
            }
        }

        logger.info("Data pipeline complete: fetched={}, transformed={}, stored={}",
                totalFetched, transformedRecords.size(), totalStored);

        // Update last sync timestamp
        connection.setUpdatedAt(Instant.now());
        connectionRepository.save(connection);

        // Step 6: Log the fetch result
        logger.info("Data fetch complete for connection: {}. Records fetched: {}, stored: {}",
                connectionId, totalFetched, totalStored);

        return new FetchResult(totalFetched, totalStored);
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        String inner = json.trim();
        if (inner.startsWith("[")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        return Arrays.stream(inner.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== Inner Classes ====================

    public static class FetchResult {

        private final int recordsFetched;
        private final int recordsStored;

        public FetchResult(int recordsFetched, int recordsStored) {
            this.recordsFetched = recordsFetched;
            this.recordsStored = recordsStored;
        }

        public int getRecordsFetched() {
            return recordsFetched;
        }

        public int getRecordsStored() {
            return recordsStored;
        }
    }
}
