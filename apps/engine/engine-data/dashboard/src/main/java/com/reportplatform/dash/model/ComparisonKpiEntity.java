package com.reportplatform.dash.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comparison_kpis")
public class ComparisonKpiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "value_field", nullable = false)
    private String valueField;

    @Column(nullable = false, length = 10)
    private String aggregation = "SUM";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "group_by", columnDefinition = "JSONB", nullable = false)
    private String groupBy = "[]";

    @Column(name = "source_type", length = 20)
    private String sourceType = "ALL";

    @Column(length = 20)
    private String normalization = "NONE";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ComparisonKpiEntity() {}

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getValueField() { return valueField; }
    public void setValueField(String valueField) { this.valueField = valueField; }
    public String getAggregation() { return aggregation; }
    public void setAggregation(String aggregation) { this.aggregation = aggregation; }
    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getNormalization() { return normalization; }
    public void setNormalization(String normalization) { this.normalization = normalization; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
