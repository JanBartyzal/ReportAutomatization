package com.reportplatform.dash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drilldown_report_definitions")
public class DrilldownReportDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType = "ANALYTICAL";

    @Column(name = "base_period_type", length = 50)
    private String basePeriodType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_filters", columnDefinition = "jsonb", nullable = false)
    private String defaultFilters = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", columnDefinition = "jsonb", nullable = false)
    private String layoutConfig = "{}";

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getBasePeriodType() {
        return basePeriodType;
    }

    public void setBasePeriodType(String basePeriodType) {
        this.basePeriodType = basePeriodType;
    }

    public String getDefaultFilters() {
        return defaultFilters;
    }

    public void setDefaultFilters(String defaultFilters) {
        this.defaultFilters = defaultFilters;
    }

    public String getLayoutConfig() {
        return layoutConfig;
    }

    public void setLayoutConfig(String layoutConfig) {
        this.layoutConfig = layoutConfig;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
