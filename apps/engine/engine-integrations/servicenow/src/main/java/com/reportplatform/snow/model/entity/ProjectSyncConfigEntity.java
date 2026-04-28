package com.reportplatform.snow.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores project-sync configuration per ServiceNow connection.
 * One connection has at most one project-sync config (unique constraint on connection_id).
 * RAG thresholds (budget utilization % and schedule variance days) are configurable per org.
 */
@Entity
@Table(name = "snow_project_sync_config")
public class ProjectSyncConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    /**
     * Controls which projects are fetched.
     * ALL – all projects; ACTIVE_ONLY – active/in-progress only; BY_MANAGER – filtered by manager email.
     */
    @Column(name = "sync_scope", nullable = false, length = 20)
    private String syncScope = "ACTIVE_ONLY";

    /** Comma-separated manager email addresses (used when syncScope = BY_MANAGER). */
    @Column(name = "filter_manager_emails", columnDefinition = "TEXT")
    private String filterManagerEmails;

    /** ISO 4217 currency code for budget values. */
    @Column(name = "budget_currency", nullable = false, length = 3)
    private String budgetCurrency = "CZK";

    /** Budget utilization % threshold for AMBER RAG status (default 80). */
    @Column(name = "rag_amber_budget_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal ragAmberBudgetThreshold = new BigDecimal("80.00");

    /** Budget utilization % threshold for RED RAG status (default 95). */
    @Column(name = "rag_red_budget_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal ragRedBudgetThreshold = new BigDecimal("95.00");

    /** Schedule variance days threshold for AMBER RAG status (default 1). */
    @Column(name = "rag_amber_schedule_days", nullable = false)
    private int ragAmberScheduleDays = 1;

    /** Schedule variance days threshold for RED RAG status (default 14). */
    @Column(name = "rag_red_schedule_days", nullable = false)
    private int ragRedScheduleDays = 14;

    @Column(name = "sync_enabled", nullable = false)
    private boolean syncEnabled = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProjectSyncConfigEntity() {}

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = connectionId; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getSyncScope() { return syncScope; }
    public void setSyncScope(String syncScope) { this.syncScope = syncScope; }

    public String getFilterManagerEmails() { return filterManagerEmails; }
    public void setFilterManagerEmails(String filterManagerEmails) { this.filterManagerEmails = filterManagerEmails; }

    public String getBudgetCurrency() { return budgetCurrency; }
    public void setBudgetCurrency(String budgetCurrency) { this.budgetCurrency = budgetCurrency; }

    public BigDecimal getRagAmberBudgetThreshold() { return ragAmberBudgetThreshold; }
    public void setRagAmberBudgetThreshold(BigDecimal ragAmberBudgetThreshold) { this.ragAmberBudgetThreshold = ragAmberBudgetThreshold; }

    public BigDecimal getRagRedBudgetThreshold() { return ragRedBudgetThreshold; }
    public void setRagRedBudgetThreshold(BigDecimal ragRedBudgetThreshold) { this.ragRedBudgetThreshold = ragRedBudgetThreshold; }

    public int getRagAmberScheduleDays() { return ragAmberScheduleDays; }
    public void setRagAmberScheduleDays(int ragAmberScheduleDays) { this.ragAmberScheduleDays = ragAmberScheduleDays; }

    public int getRagRedScheduleDays() { return ragRedScheduleDays; }
    public void setRagRedScheduleDays(int ragRedScheduleDays) { this.ragRedScheduleDays = ragRedScheduleDays; }

    public boolean isSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
