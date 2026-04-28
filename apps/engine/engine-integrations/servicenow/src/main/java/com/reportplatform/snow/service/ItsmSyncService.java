package com.reportplatform.snow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.base.dapr.DaprClientWrapper;
import com.reportplatform.snow.model.dto.ItsmSummaryDto;
import com.reportplatform.snow.model.dto.ResolverGroupDto;
import com.reportplatform.snow.model.dto.CreateResolverGroupRequest;
import com.reportplatform.snow.model.dto.ServiceNowTableDataDTO;
import com.reportplatform.snow.model.entity.ResolverGroupConfigEntity;
import com.reportplatform.snow.model.entity.ServiceNowConnectionEntity;
import com.reportplatform.snow.repository.ResolverGroupConfigRepository;
import com.reportplatform.snow.repository.ServiceNowConnectionRepository;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fetches Incidents and Requests from ServiceNow filtered by configured
 * resolver groups (assignment_group). Calculates derived ITSM metrics
 * and stores results to dedicated typed tables via ms-sink-tbl.
 */
@Service
public class ItsmSyncService {

    private static final Logger log = LoggerFactory.getLogger(ItsmSyncService.class);

    /**
     * ServiceNow returns datetimes as "yyyy-MM-dd HH:mm:ss" in UTC when
     * sysparm_display_value=false (the default for table API calls).
     * This formatter parses that format directly.
     */
    private static final DateTimeFormatter SN_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    /** SN fields to request for incidents – reduces payload vs. full record fetch. */
    static final String INCIDENT_FIELDS =
            "number,short_description,description,state,priority,urgency,impact," +
            "category,subcategory,assignment_group,assigned_to,opened_by," +
            "opened_at,resolved_at,close_code,u_sla_breach,sla_due," +
            "sys_updated_on,sys_id";

    /** SN fields to request for requests. */
    static final String REQUEST_FIELDS =
            "number,short_description,state,approval,stage," +
            "requested_for,requested_by,opened_at,due_date,closed_at," +
            "assignment_group,sys_updated_on,sys_id";

    private static final int PAGE_SIZE = 100;

    private final ResolverGroupConfigRepository groupRepository;
    private final ServiceNowConnectionRepository connectionRepository;
    private final ServiceNowClient serviceNowClient;
    private final KeyVaultService keyVaultService;
    private final DaprClientWrapper daprClientWrapper;
    private final ObjectMapper objectMapper;

    /**
     * Self-reference injected lazily to allow {@link #syncGroup} calls from
     * {@link #syncAllGroupsForConnection} to go through the Spring proxy,
     * so that each group sync runs in its own independent transaction.
     */
    @Lazy
    @Autowired
    private ItsmSyncService self;

    @Value("${dapr.remote.ms-sink-tbl-app-id:ms-sink-tbl}")
    private String msSinkTblAppId;

    public ItsmSyncService(ResolverGroupConfigRepository groupRepository,
                           ServiceNowConnectionRepository connectionRepository,
                           ServiceNowClient serviceNowClient,
                           KeyVaultService keyVaultService,
                           DaprClientWrapper daprClientWrapper,
                           ObjectMapper objectMapper) {
        this.groupRepository = groupRepository;
        this.connectionRepository = connectionRepository;
        this.serviceNowClient = serviceNowClient;
        this.keyVaultService = keyVaultService;
        this.daprClientWrapper = daprClientWrapper;
        this.objectMapper = objectMapper;
    }

    // ---- Resolver Group CRUD ----

    @Transactional(readOnly = true)
    public List<ResolverGroupDto> listGroups(UUID connectionId) {
        return groupRepository.findByConnectionId(connectionId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ResolverGroupDto createGroup(UUID connectionId, UUID orgId, CreateResolverGroupRequest req) {
        if (groupRepository.existsByConnectionIdAndGroupSysId(connectionId, req.groupSysId())) {
            throw new IllegalArgumentException(
                    "Resolver group '" + req.groupSysId() + "' already registered for this connection.");
        }
        ResolverGroupConfigEntity entity = new ResolverGroupConfigEntity();
        entity.setConnectionId(connectionId);
        entity.setOrgId(orgId);
        entity.setGroupSysId(req.groupSysId());
        entity.setGroupName(req.groupName());
        entity.setDataTypes(req.dataTypes());
        entity.setSyncEnabled(req.syncEnabled());
        return toDto(groupRepository.save(entity));
    }

    @Transactional
    public void deleteGroup(UUID connectionId, UUID groupId) {
        ResolverGroupConfigEntity entity = groupRepository.findById(groupId)
                .filter(g -> g.getConnectionId().equals(connectionId))
                .orElseThrow(() -> new NoSuchElementException("Resolver group not found: " + groupId));
        groupRepository.delete(entity);
    }

    // ---- ITSM Sync ----

    /**
     * Fetches ITSM data for a specific resolver group and stores it
     * to the dedicated snow_incidents / snow_requests tables.
     *
     * @param connectionId  ServiceNow connection UUID
     * @param groupId       Resolver group UUID
     * @param incrementalTs Optional ISO timestamp for incremental sync
     * @return sync result with counts
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncResult syncGroup(UUID connectionId, UUID groupId, String incrementalTs) {
        ResolverGroupConfigEntity group = groupRepository.findById(groupId)
                .filter(g -> g.getConnectionId().equals(connectionId))
                .orElseThrow(() -> new NoSuchElementException("Resolver group not found: " + groupId));

        ServiceNowConnectionEntity connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new NoSuchElementException("Connection not found: " + connectionId));

        String credentials = keyVaultService.getSecret(connection.getCredentialsRef());
        String token = serviceNowClient.authenticate(
                connection.getInstanceUrl(), connection.getAuthType(), credentials);

        List<String> dataTypes = parseJsonArray(group.getDataTypes());
        int totalFetched = 0;
        int totalStored = 0;

        for (String dataType : dataTypes) {
            SyncResult partial = switch (dataType.toUpperCase()) {
                case "INCIDENT" -> fetchAndStore(
                        connection, group, token, "incident",
                        INCIDENT_FIELDS, "snow_incidents", incrementalTs);
                case "REQUEST" -> fetchAndStore(
                        connection, group, token, "sc_request",
                        REQUEST_FIELDS, "snow_requests", incrementalTs);
                case "TASK" -> fetchAndStore(
                        connection, group, token, "sc_task",
                        REQUEST_FIELDS, "snow_requests", incrementalTs);
                default -> {
                    log.warn("Unknown data type '{}' for group {}", dataType, groupId);
                    yield new SyncResult(0, 0);
                }
            };
            totalFetched += partial.fetched();
            totalStored += partial.stored();
        }

        group.setLastSyncedAt(Instant.now());
        groupRepository.save(group);

        log.info("ITSM sync complete for group '{}': fetched={}, stored={}",
                group.getGroupName(), totalFetched, totalStored);
        return new SyncResult(totalFetched, totalStored);
    }

    /**
     * Sync all enabled groups for a connection.
     * <p>
     * Each group runs in its own independent transaction (REQUIRES_NEW on
     * {@link #syncGroup}). A failure in one group is logged and counted but
     * does not abort the remaining groups — best-effort semantics.
     * </p>
     */
    public SyncResult syncAllGroupsForConnection(UUID connectionId, String incrementalTs) {
        List<ResolverGroupConfigEntity> groups =
                groupRepository.findByConnectionIdAndSyncEnabledTrue(connectionId);
        int totalFetched = 0, totalStored = 0;
        for (ResolverGroupConfigEntity group : groups) {
            try {
                // Call via self-proxy so that REQUIRES_NEW propagation takes effect
                SyncResult r = self.syncGroup(connectionId, group.getId(), incrementalTs);
                totalFetched += r.fetched();
                totalStored += r.stored();
            } catch (Exception e) {
                log.error("ITSM sync failed for group '{}' ({}): {}",
                        group.getGroupName(), group.getId(), e.getMessage(), e);
            }
        }
        return new SyncResult(totalFetched, totalStored);
    }

    // ---- ITSM Summary (read from sink, not from SN) ----

    public ItsmSummaryDto buildSummary(UUID connectionId, UUID groupId) {
        ResolverGroupConfigEntity group = groupRepository.findById(groupId)
                .filter(g -> g.getConnectionId().equals(connectionId))
                .orElseThrow(() -> new NoSuchElementException("Resolver group not found: " + groupId));

        // Delegate to sink query via Dapr – returns aggregated row
        @SuppressWarnings("unchecked")
        Map<String, Object> result = daprClientWrapper.invokeMethod(
                msSinkTblAppId,
                "/api/v1/itsm/summary?connectionId=" + connectionId + "&groupId=" + groupId,
                null,
                HttpExtension.GET,
                new TypeRef<Map<String, Object>>() {})
                .block();

        if (result == null) {
            return new ItsmSummaryDto(connectionId, groupId, group.getGroupName(),
                    0, 0, 0, 0.0, 0.0, 0, 0, 0, 0, 0.0, Instant.now());
        }

        return new ItsmSummaryDto(
                connectionId, groupId, group.getGroupName(),
                toLong(result, "incidentOpenCount"),
                toLong(result, "incidentResolvedCount"),
                toLong(result, "incidentTotalCount"),
                toDouble(result, "incidentSlaBreachPct"),
                toDouble(result, "incidentAvgResolutionHours"),
                toLong(result, "incidentCriticalOpenCount"),
                toLong(result, "requestOpenCount"),
                toLong(result, "requestClosedCount"),
                toLong(result, "requestTotalCount"),
                toDouble(result, "requestAvgAgeHours"),
                Instant.now()
        );
    }

    // ---- Private helpers ----

    private SyncResult fetchAndStore(ServiceNowConnectionEntity connection,
                                      ResolverGroupConfigEntity group,
                                      String token,
                                      String tableName,
                                      String fields,
                                      String targetTable,
                                      String incrementalTs) {
        String baseQuery = "assignment_group.sys_id=" + group.getGroupSysId();
        if (incrementalTs != null && !incrementalTs.isBlank()) {
            baseQuery += "^sys_updated_on>" + incrementalTs;
        }

        List<ServiceNowTableDataDTO> allRecords = new ArrayList<>();
        int offset = 0;

        while (true) {
            List<ServiceNowTableDataDTO> page = serviceNowClient.fetchTable(
                    connection.getInstanceUrl(), tableName, token,
                    offset, PAGE_SIZE, incrementalTs, fields, baseQuery);

            if (page.isEmpty()) break;

            // Enrich with derived metrics before storing
            page.forEach(record -> enrichItsmRecord(record, tableName, group));
            allRecords.addAll(page);
            offset += page.size();
            if (page.size() < PAGE_SIZE) break;
        }

        if (allRecords.isEmpty()) return new SyncResult(0, 0);

        // Persist to dedicated typed table via ms-sink-tbl
        Map<String, Object> persistRequest = Map.of(
                "targetTable", targetTable,
                "connectionId", connection.getId().toString(),
                "resolverGroupId", group.getId().toString(),
                "orgId", group.getOrgId().toString(),
                "records", allRecords.stream()
                        .map(ServiceNowTableDataDTO::getFields)
                        .collect(Collectors.toList()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = daprClientWrapper.invokeMethod(
                msSinkTblAppId,
                "/api/v1/itsm/upsert",
                persistRequest,
                HttpExtension.POST,
                new TypeRef<Map<String, Object>>() {})
                .block();

        int stored = response != null
                ? ((Number) response.getOrDefault("recordsUpserted", 0)).intValue()
                : 0;

        return new SyncResult(allRecords.size(), stored);
    }

    /** Calculates derived fields and adds them to the record's field map. */
    private void enrichItsmRecord(ServiceNowTableDataDTO record, String tableName,
                                   ResolverGroupConfigEntity group) {
        Map<String, Object> fields = record.getFields();
        if (fields == null) return;

        // Add resolver group context
        fields.put("_resolver_group_id", group.getId().toString());
        fields.put("_resolver_group_name", group.getGroupName());

        // Calculate resolution time (hours) for incidents
        if ("incident".equals(tableName)) {
            String openedAt = str(fields, "opened_at");
            String resolvedAt = str(fields, "resolved_at");
            if (openedAt != null && resolvedAt != null) {
                try {
                    Instant opened   = parseSnDatetime(openedAt);
                    Instant resolved = parseSnDatetime(resolvedAt);
                    double hours = (resolved.toEpochMilli() - opened.toEpochMilli()) / 3_600_000.0;
                    fields.put("_resolution_time_hours", Math.max(0, hours));
                } catch (Exception e) {
                    log.warn("Failed to calculate resolution time for incident (opened={}, resolved={}): {}",
                            openedAt, resolvedAt, e.getMessage());
                }
            }
            // Map SN sla_breach field
            Object slaBreach = fields.get("u_sla_breach");
            fields.put("_is_sla_breached",
                    "true".equalsIgnoreCase(String.valueOf(slaBreach)));
        }

        // Age in days from opened_at to now
        String openedAt = str(fields, "opened_at");
        if (openedAt != null) {
            try {
                Instant opened = parseSnDatetime(openedAt);
                double ageDays = (Instant.now().toEpochMilli() - opened.toEpochMilli()) / 86_400_000.0;
                fields.put("_age_days", Math.max(0, ageDays));
            } catch (Exception e) {
                log.warn("Failed to calculate age_days for record (opened_at={}): {}", openedAt, e.getMessage());
            }
        }
    }

    private String str(Map<String, Object> fields, String key) {
        Object v = fields.get(key);
        if (v instanceof Map<?, ?> ref) {
            // SN reference fields come as {display_value: "...", value: "..."}
            Object val = ref.get("value");
            return val != null ? val.toString() : null;
        }
        return v != null ? v.toString() : null;
    }

    /**
     * Deserializes a JSONB-stored string array (e.g. {@code ["INCIDENT","REQUEST"]})
     * using Jackson — handles escaped characters and whitespace correctly.
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse data_types JSON '{}': {}", json, e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses a ServiceNow datetime string to {@link Instant}.
     * <p>
     * Accepts both the canonical SN format ({@code "yyyy-MM-dd HH:mm:ss"} in UTC)
     * and ISO-8601 ({@code "2024-03-15T10:30:00Z"}).
     * </p>
     */
    private static Instant parseSnDatetime(String value) {
        if (value == null) throw new DateTimeParseException("null datetime value", "", 0);
        try {
            // Primary: SN table API default format – "yyyy-MM-dd HH:mm:ss" UTC
            return SN_DATETIME_FORMATTER.parse(value.trim(), Instant::from);
        } catch (DateTimeParseException e) {
            // Fallback: ISO-8601 (e.g. when sysparm_display_value=true is used)
            return Instant.parse(value.trim());
        }
    }

    private long toLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private double toDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private ResolverGroupDto toDto(ResolverGroupConfigEntity e) {
        return new ResolverGroupDto(e.getId(), e.getConnectionId(), e.getOrgId(),
                e.getGroupSysId(), e.getGroupName(), e.getDataTypes(),
                e.isSyncEnabled(), e.getLastSyncedAt(), e.getCreatedAt());
    }

    // ---- Inner records ----

    public record SyncResult(int fetched, int stored) {}
}
