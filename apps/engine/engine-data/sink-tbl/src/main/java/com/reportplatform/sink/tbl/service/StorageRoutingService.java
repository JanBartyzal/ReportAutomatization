package com.reportplatform.sink.tbl.service;

import com.reportplatform.sink.tbl.backend.PostgresTableStorageBackend;
import com.reportplatform.sink.tbl.backend.TableStorageBackend;
import com.reportplatform.sink.tbl.entity.StorageRoutingConfigEntity;
import com.reportplatform.sink.tbl.repository.StorageRoutingConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves which {@link TableStorageBackend} should handle a write request
 * for a given (orgId, sourceType) pair.
 *
 * <p>Rules are loaded once from the DB at startup and refreshed every
 * 5 minutes so that an admin configuration change takes effect without
 * requiring a service restart.</p>
 *
 * <p>Specificity order (first match wins):
 * <ol>
 *   <li>org + source_type</li>
 *   <li>org only (source_type wildcard)</li>
 *   <li>source_type only (org wildcard)</li>
 *   <li>global default (both NULL)</li>
 *   <li>hard fallback → POSTGRES</li>
 * </ol>
 * </p>
 */
@Service
public class StorageRoutingService {

    private static final Logger logger = LoggerFactory.getLogger(StorageRoutingService.class);

    private final StorageRoutingConfigRepository repository;
    private final Map<String, TableStorageBackend> backendRegistry;

    /** Ordered rules snapshot – refreshed periodically */
    private volatile List<StorageRoutingConfigEntity> rules = List.of();

    public StorageRoutingService(
            StorageRoutingConfigRepository repository,
            List<TableStorageBackend> backends) {
        this.repository = repository;
        this.backendRegistry = backends.stream()
                .collect(Collectors.toMap(TableStorageBackend::backendType, Function.identity()));
        refreshRules();
    }

    /**
     * Resolves the active backend for the given org and source type.
     *
     * @param orgId      organisation UUID string (may be null)
     * @param sourceType e.g. "EXCEL", "PPTX", "SERVICE_NOW" (may be null)
     * @return active {@link TableStorageBackend}, never null (falls back to POSTGRES)
     */
    public TableStorageBackend resolve(String orgId, String sourceType) {
        UUID orgUuid = parseUuidSilently(orgId);

        for (StorageRoutingConfigEntity rule : rules) {
            if (matches(rule, orgUuid, sourceType)) {
                String backend = rule.getBackend();
                TableStorageBackend impl = backendRegistry.get(backend);
                if (impl != null) {
                    logger.debug("Routing [{}/{}] → {} (rule id={})",
                            orgId, sourceType, backend, rule.getId());
                    return impl;
                }
                logger.warn("Backend '{}' from rule {} not registered; falling back to POSTGRES", backend, rule.getId());
            }
        }

        logger.debug("No routing rule matched for [{}/{}]; defaulting to POSTGRES", orgId, sourceType);
        TableStorageBackend postgres = backendRegistry.get(PostgresTableStorageBackend.BACKEND_TYPE);
        if (postgres != null) {
            return postgres;
        }
        throw new IllegalStateException(
                "No storage backends registered. At minimum PostgresTableStorageBackend must be present. " +
                "Registered backends: " + backendRegistry.keySet());
    }

    /** Reload rules from DB (called on startup and every 5 minutes). */
    @Scheduled(fixedDelayString = "${storage.routing.refresh-ms:300000}")
    public void refreshRules() {
        try {
            rules = repository.findAllEffective(OffsetDateTime.now());
            logger.info("Storage routing rules refreshed: {} rule(s) loaded", rules.size());
        } catch (Exception e) {
            logger.error("Failed to refresh storage routing rules: {}", e.getMessage(), e);
        }
    }

    // --- helpers ---

    private boolean matches(StorageRoutingConfigEntity rule, UUID orgUuid, String sourceType) {
        boolean orgMatch = rule.getOrgId() == null || rule.getOrgId().equals(orgUuid);
        boolean srcMatch = rule.getSourceType() == null
                || rule.getSourceType().equalsIgnoreCase(sourceType);
        return orgMatch && srcMatch;
    }

    private static UUID parseUuidSilently(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
