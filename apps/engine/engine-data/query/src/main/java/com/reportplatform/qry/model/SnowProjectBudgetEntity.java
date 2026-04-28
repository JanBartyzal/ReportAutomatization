package com.reportplatform.qry.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "snow_project_budgets")
public class SnowProjectBudgetEntity {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "sys_id", length = 32)
    private String sysId;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "fiscal_year", length = 10)
    private String fiscalYear;

    @Column(name = "planned_amount", precision = 18, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "actual_amount", precision = 18, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "synced_at")
    private Instant syncedAt;

    // ---- Getters ----

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getProjectId() { return projectId; }
    public String getSysId() { return sysId; }
    public String getCategory() { return category; }
    public String getFiscalYear() { return fiscalYear; }
    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public Instant getSyncedAt() { return syncedAt; }
}
