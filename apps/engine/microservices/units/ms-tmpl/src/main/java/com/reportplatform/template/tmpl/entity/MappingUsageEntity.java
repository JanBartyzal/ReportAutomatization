package com.reportplatform.template.tmpl.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for the mapping_usage_tracking table.
 * Tracks how often a mapping template is used per organization,
 * enabling smart persistence promotion detection.
 */
@Entity
@Table(name = "mapping_usage_tracking",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mapping_template_id", "org_id"}))
public class MappingUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "mapping_template_id", nullable = false)
    private UUID mappingTemplateId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "usage_count", nullable = false)
    private long usageCount = 0;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "distinct_org_count")
    private int distinctOrgCount = 1;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMappingTemplateId() { return mappingTemplateId; }
    public void setMappingTemplateId(UUID mappingTemplateId) { this.mappingTemplateId = mappingTemplateId; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }

    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public int getDistinctOrgCount() { return distinctOrgCount; }
    public void setDistinctOrgCount(int distinctOrgCount) { this.distinctOrgCount = distinctOrgCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
