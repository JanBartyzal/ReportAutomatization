package com.reportplatform.snow.service;

import com.reportplatform.base.dapr.DaprClientWrapper;
import com.reportplatform.snow.model.dto.ProjectSyncConfigDto;
import com.reportplatform.snow.model.dto.ServiceNowTableDataDTO;
import com.reportplatform.snow.model.dto.UpsertProjectSyncConfigRequest;
import com.reportplatform.snow.model.entity.ProjectSyncConfigEntity;
import com.reportplatform.snow.model.entity.ServiceNowConnectionEntity;
import com.reportplatform.snow.repository.ProjectSyncConfigRepository;
import com.reportplatform.snow.repository.ServiceNowConnectionRepository;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches project data from ServiceNow (pm_project, pm_project_task, pm_project_budget_plan)
 * and persists it to dedicated typed tables via ms-sink-tbl.
 * Calculates RAG status and KPI metrics during sync.
 */
@Service
public class ProjectFetchService {

    private static final Logger log = LoggerFactory.getLogger(ProjectFetchService.class);

    private static final String PROJECT_FIELDS =
            "sys_id,number,short_description,status,phase,start_date,end_date," +
            "percent_complete,total_budget,actual_cost,projected_cost,department," +
            "manager,sys_updated_on";

    private static final String TASK_FIELDS =
            "sys_id,number,short_description,state,milestone,due_date,assigned_to," +
            "parent,project,closed_at,sys_updated_on";

    private static final String BUDGET_FIELDS =
            "sys_id,project,planned_amount,actual_amount,category,fiscal_year";

    private static final int PAGE_SIZE = 100;

    private final ProjectSyncConfigRepository configRepository;
    private final ServiceNowConnectionRepository connectionRepository;
    private final ServiceNowClient serviceNowClient;
    private final KeyVaultService keyVaultService;
    private final DaprClientWrapper daprClientWrapper;

    @Value("${dapr.remote.ms-sink-tbl-app-id:ms-sink-tbl}")
    private String msSinkTblAppId;

    public ProjectFetchService(ProjectSyncConfigRepository configRepository,
                               ServiceNowConnectionRepository connectionRepository,
                               ServiceNowClient serviceNowClient,
                               KeyVaultService keyVaultService,
                               DaprClientWrapper daprClientWrapper) {
        this.configRepository = configRepository;
        this.connectionRepository = connectionRepository;
        this.serviceNowClient = serviceNowClient;
        this.keyVaultService = keyVaultService;
        this.daprClientWrapper = daprClientWrapper;
    }

    // ---- Config CRUD ----

    @Transactional(readOnly = true)
    public Optional<ProjectSyncConfigDto> getConfig(UUID connectionId) {
        return configRepository.findByConnectionId(connectionId).map(this::toDto);
    }

    @Transactional
    public ProjectSyncConfigDto upsertConfig(UUID connectionId, UUID orgId,
                                              UpsertProjectSyncConfigRequest req) {
        ProjectSyncConfigEntity entity = configRepository.findByConnectionId(connectionId)
                .orElseGet(() -> {
                    ProjectSyncConfigEntity e = new ProjectSyncConfigEntity();
                    e.setConnectionId(connectionId);
                    e.setOrgId(orgId);
                    return e;
                });

        if (req.syncScope() != null) entity.setSyncScope(req.syncScope());
        if (req.filterManagerEmails() != null) entity.setFilterManagerEmails(req.filterManagerEmails());
        if (req.budgetCurrency() != null) entity.setBudgetCurrency(req.budgetCurrency());
        if (req.ragAmberBudgetThreshold() != null) entity.setRagAmberBudgetThreshold(req.ragAmberBudgetThreshold());
        if (req.ragRedBudgetThreshold() != null) entity.setRagRedBudgetThreshold(req.ragRedBudgetThreshold());
        if (req.ragAmberScheduleDays() != null) entity.setRagAmberScheduleDays(req.ragAmberScheduleDays());
        if (req.ragRedScheduleDays() != null) entity.setRagRedScheduleDays(req.ragRedScheduleDays());
        if (req.syncEnabled() != null) entity.setSyncEnabled(req.syncEnabled());

        return toDto(configRepository.save(entity));
    }

    // ---- Sync ----

    /**
     * Fetches projects (and their tasks + budget lines) from ServiceNow,
     * calculates KPIs, and persists via Dapr → ms-sink-tbl.
     *
     * @return total projects synced
     */
    @Transactional
    public SyncResult syncProjects(UUID connectionId) {
        ProjectSyncConfigEntity config = configRepository.findByConnectionId(connectionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No project sync config for connection: " + connectionId));

        if (!config.isSyncEnabled()) {
            log.info("Project sync disabled for connection {}", connectionId);
            return new SyncResult(0, 0);
        }

        ServiceNowConnectionEntity connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new NoSuchElementException("Connection not found: " + connectionId));

        String credentials = keyVaultService.getSecret(connection.getCredentialsRef());
        String token = serviceNowClient.authenticate(
                connection.getInstanceUrl(), connection.getAuthType(), credentials);

        String projectQuery = buildProjectQuery(config);
        List<ServiceNowTableDataDTO> projects = fetchAllPages(
                connection.getInstanceUrl(), "pm_project", token, PROJECT_FIELDS, projectQuery);

        log.info("Fetched {} projects for connection {}", projects.size(), connectionId);

        int totalStored = 0;
        for (ServiceNowTableDataDTO projectRecord : projects) {
            String projectSysId = str(projectRecord.getFields(), "sys_id");
            if (projectSysId == null || projectSysId.isBlank()) continue;

            // Fetch tasks and budget lines for this project
            String taskQuery = "project=" + projectSysId;
            List<ServiceNowTableDataDTO> tasks = fetchAllPages(
                    connection.getInstanceUrl(), "pm_project_task", token, TASK_FIELDS, taskQuery);

            String budgetQuery = "project=" + projectSysId;
            List<ServiceNowTableDataDTO> budgets = fetchAllPages(
                    connection.getInstanceUrl(), "pm_project_budget_plan", token, BUDGET_FIELDS, budgetQuery);

            // Enrich project record with derived KPIs
            enrichProjectRecord(projectRecord.getFields(), tasks, config);

            // Persist the full project tree
            Map<String, Object> persistRequest = Map.of(
                    "connectionId", connectionId.toString(),
                    "orgId", config.getOrgId().toString(),
                    "project", projectRecord.getFields(),
                    "tasks", tasks.stream().map(ServiceNowTableDataDTO::getFields).collect(Collectors.toList()),
                    "budgets", budgets.stream().map(ServiceNowTableDataDTO::getFields).collect(Collectors.toList())
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = daprClientWrapper.invokeMethod(
                    msSinkTblAppId,
                    "/api/v1/projects/upsert",
                    persistRequest,
                    HttpExtension.POST,
                    new TypeRef<Map<String, Object>>() {})
                    .block();

            if (response != null) {
                totalStored += ((Number) response.getOrDefault("recordsUpserted", 1)).intValue();
            }
        }

        config.setLastSyncedAt(Instant.now());
        configRepository.save(config);

        log.info("Project sync complete for connection {}: fetched={}, stored={}",
                connectionId, projects.size(), totalStored);
        return new SyncResult(projects.size(), totalStored);
    }

    // ---- Private helpers ----

    private String buildProjectQuery(ProjectSyncConfigEntity config) {
        return switch (config.getSyncScope()) {
            case "ACTIVE_ONLY" -> "statusINactive,in_planning,on_hold";
            case "BY_MANAGER" -> buildManagerFilter(config.getFilterManagerEmails());
            default -> "";  // ALL – no filter
        };
    }

    private String buildManagerFilter(String emailsCsv) {
        if (emailsCsv == null || emailsCsv.isBlank()) return "";
        String[] emails = emailsCsv.split(",");
        if (emails.length == 1) {
            return "manager.email=" + emails[0].trim();
        }
        // SN uses IN operator with ^OR for multiple values
        return Arrays.stream(emails)
                .map(e -> "manager.email=" + e.trim())
                .collect(Collectors.joining("^OR"));
    }

    private List<ServiceNowTableDataDTO> fetchAllPages(String instanceUrl, String tableName,
                                                        String token, String fields, String query) {
        List<ServiceNowTableDataDTO> all = new ArrayList<>();
        int offset = 0;
        while (true) {
            List<ServiceNowTableDataDTO> page = serviceNowClient.fetchTable(
                    instanceUrl, tableName, token, offset, PAGE_SIZE, null, fields, query);
            if (page.isEmpty()) break;
            all.addAll(page);
            offset += page.size();
            if (page.size() < PAGE_SIZE) break;
        }
        return all;
    }

    /**
     * Calculates and adds derived KPI fields to the project field map.
     * These are stored as _kpi_* prefixed fields so the sink service can map them
     * to the typed columns in snow_projects.
     */
    private void enrichProjectRecord(Map<String, Object> fields,
                                      List<ServiceNowTableDataDTO> tasks,
                                      ProjectSyncConfigEntity config) {
        // Budget utilization
        BigDecimal totalBudget = toBigDecimal(fields, "total_budget");
        BigDecimal actualCost = toBigDecimal(fields, "actual_cost");
        BigDecimal projectedCost = toBigDecimal(fields, "projected_cost");

        BigDecimal budgetUtil = null;
        if (totalBudget != null && totalBudget.compareTo(BigDecimal.ZERO) > 0 && actualCost != null) {
            budgetUtil = actualCost.divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            fields.put("_kpi_budget_utilization_pct", budgetUtil);
        }

        // Cost forecast accuracy
        if (totalBudget != null && totalBudget.compareTo(BigDecimal.ZERO) > 0 && projectedCost != null) {
            BigDecimal accuracy = projectedCost.divide(totalBudget, 4, RoundingMode.HALF_UP);
            fields.put("_kpi_cost_forecast_accuracy", accuracy);
        }

        // Schedule variance (projected_end - planned_end in days)
        Integer scheduleVariance = null;
        String plannedEnd = str(fields, "end_date");
        String projectedEnd = str(fields, "projected_end_date");
        if (plannedEnd != null && projectedEnd != null) {
            try {
                LocalDate planned = LocalDate.parse(plannedEnd.substring(0, 10));
                LocalDate projected = LocalDate.parse(projectedEnd.substring(0, 10));
                scheduleVariance = (int) (projected.toEpochDay() - planned.toEpochDay());
                fields.put("_kpi_schedule_variance_days", scheduleVariance);
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse schedule dates (plannedEnd={}, projectedEnd={}): {}", plannedEnd, projectedEnd, e.getMessage());
            }
        }

        // Milestone completion rate
        long totalMilestones = tasks.stream()
                .filter(t -> isMilestone(t.getFields()))
                .count();
        long completedMilestones = tasks.stream()
                .filter(t -> isMilestone(t.getFields()) && isTaskComplete(t.getFields()))
                .count();
        if (totalMilestones > 0) {
            BigDecimal milestoneRate = BigDecimal.valueOf(completedMilestones)
                    .divide(BigDecimal.valueOf(totalMilestones), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            fields.put("_kpi_milestone_completion_rate", milestoneRate);
        }

        // RAG status
        String ragStatus = calculateRag(budgetUtil, scheduleVariance, config);
        fields.put("_kpi_rag_status", ragStatus);
    }

    private String calculateRag(BigDecimal budgetUtil, Integer scheduleVariance,
                                 ProjectSyncConfigEntity config) {
        boolean redBudget = budgetUtil != null
                && budgetUtil.compareTo(config.getRagRedBudgetThreshold()) > 0;
        boolean redSchedule = scheduleVariance != null
                && scheduleVariance > config.getRagRedScheduleDays();

        if (redBudget || redSchedule) return "RED";

        boolean amberBudget = budgetUtil != null
                && budgetUtil.compareTo(config.getRagAmberBudgetThreshold()) > 0;
        boolean amberSchedule = scheduleVariance != null
                && scheduleVariance >= config.getRagAmberScheduleDays();

        if (amberBudget || amberSchedule) return "AMBER";

        return "GREEN";
    }

    private boolean isMilestone(Map<String, Object> fields) {
        Object v = fields.get("milestone");
        return "true".equalsIgnoreCase(String.valueOf(v)) || Boolean.TRUE.equals(v);
    }

    private boolean isTaskComplete(Map<String, Object> fields) {
        String state = str(fields, "state");
        return state != null && (state.equals("3")
                || state.equalsIgnoreCase("closed_complete")
                || state.equalsIgnoreCase("complete"));
    }

    private String str(Map<String, Object> fields, String key) {
        Object v = fields.get(key);
        if (v instanceof Map<?, ?> ref) {
            Object val = ref.get("value");
            return val != null ? val.toString() : null;
        }
        return v != null ? v.toString() : null;
    }

    private BigDecimal toBigDecimal(Map<String, Object> fields, String key) {
        String s = str(fields, key);
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) {
            log.warn("Cannot parse BigDecimal value '{}' for field '{}', treating as null", s, key);
            return null;
        }
    }

    private ProjectSyncConfigDto toDto(ProjectSyncConfigEntity e) {
        return new ProjectSyncConfigDto(
                e.getId(), e.getConnectionId(), e.getOrgId(),
                e.getSyncScope(), e.getFilterManagerEmails(), e.getBudgetCurrency(),
                e.getRagAmberBudgetThreshold(), e.getRagRedBudgetThreshold(),
                e.getRagAmberScheduleDays(), e.getRagRedScheduleDays(),
                e.isSyncEnabled(), e.getLastSyncedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public record SyncResult(int fetched, int stored) {}
}
