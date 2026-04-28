package com.reportplatform.qry.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only JPA mapping for snow_projects.
 * Written by ms-sink-tbl (ProjectFetchService → Dapr upsert), read by this engine-data query module.
 */
@Entity
@Table(name = "snow_projects")
public class SnowProjectEntity {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "resolver_connection_id", nullable = false)
    private UUID resolverConnectionId;

    @Column(name = "sys_id", length = 32)
    private String sysId;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "manager_sys_id", length = 32)
    private String managerSysId;

    @Column(name = "manager_name", length = 255)
    private String managerName;

    @Column(name = "manager_email", length = 255)
    private String managerEmail;

    @Column(name = "department", length = 255)
    private String department;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "projected_end_date")
    private LocalDate projectedEndDate;

    @Column(name = "percent_complete", precision = 5, scale = 2)
    private BigDecimal percentComplete;

    @Column(name = "total_budget", precision = 18, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "actual_cost", precision = 18, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "projected_cost", precision = 18, scale = 2)
    private BigDecimal projectedCost;

    @Column(name = "budget_utilization_pct", precision = 7, scale = 2)
    private BigDecimal budgetUtilizationPct;

    @Column(name = "schedule_variance_days")
    private Integer scheduleVarianceDays;

    @Column(name = "milestone_completion_rate", precision = 5, scale = 2)
    private BigDecimal milestoneCompletionRate;

    @Column(name = "cost_forecast_accuracy", precision = 7, scale = 4)
    private BigDecimal costForecastAccuracy;

    @Column(name = "rag_status", length = 10)
    private String ragStatus;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "sn_updated_at")
    private Instant snUpdatedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // ---- Getters ----

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getResolverConnectionId() { return resolverConnectionId; }
    public String getSysId() { return sysId; }
    public String getNumber() { return number; }
    public String getShortDescription() { return shortDescription; }
    public String getStatus() { return status; }
    public String getPhase() { return phase; }
    public String getManagerSysId() { return managerSysId; }
    public String getManagerName() { return managerName; }
    public String getManagerEmail() { return managerEmail; }
    public String getDepartment() { return department; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public LocalDate getActualStartDate() { return actualStartDate; }
    public LocalDate getProjectedEndDate() { return projectedEndDate; }
    public BigDecimal getPercentComplete() { return percentComplete; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public BigDecimal getActualCost() { return actualCost; }
    public BigDecimal getProjectedCost() { return projectedCost; }
    public BigDecimal getBudgetUtilizationPct() { return budgetUtilizationPct; }
    public Integer getScheduleVarianceDays() { return scheduleVarianceDays; }
    public BigDecimal getMilestoneCompletionRate() { return milestoneCompletionRate; }
    public BigDecimal getCostForecastAccuracy() { return costForecastAccuracy; }
    public String getRagStatus() { return ragStatus; }
    public Instant getSyncedAt() { return syncedAt; }
    public Instant getSnUpdatedAt() { return snUpdatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
