package com.reportplatform.snow.service;

import com.reportplatform.snow.model.dto.ServiceNowTableDataDTO;
import com.reportplatform.snow.model.entity.AuthType;
import com.reportplatform.snow.model.entity.ServiceNowConnectionEntity;
import com.reportplatform.snow.repository.ServiceNowConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DataFetchService {

    private static final Logger logger = LoggerFactory.getLogger(DataFetchService.class);

    private static final int PAGE_SIZE = 100;

    private final ServiceNowConnectionRepository connectionRepository;
    private final ServiceNowClient serviceNowClient;
    private final KeyVaultService keyVaultService;

    public DataFetchService(ServiceNowConnectionRepository connectionRepository,
                            ServiceNowClient serviceNowClient,
                            KeyVaultService keyVaultService) {
        this.connectionRepository = connectionRepository;
        this.serviceNowClient = serviceNowClient;
        this.keyVaultService = keyVaultService;
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

        // TODO: Send fetched data to MS-TMPL via Dapr gRPC for template matching/transformation
        // TODO: Send transformed data to MS-SINK-TBL via Dapr gRPC for persistence
        // For now, count stored = fetched (will be adjusted when sinks are wired)
        totalStored = totalFetched;

        // Update last sync timestamp
        connection.setUpdatedAt(Instant.now());
        connectionRepository.save(connection);

        // Step 5: Log the fetch result
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
