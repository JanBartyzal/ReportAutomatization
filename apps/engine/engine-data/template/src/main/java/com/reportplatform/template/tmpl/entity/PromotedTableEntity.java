package com.reportplatform.template.tmpl.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for the promoted_tables_registry table.
 * Tracks mapping templates that have been promoted to dedicated SQL tables.
 */
@Entity
@Table(name = "promoted_tables_registry")
public class PromotedTableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "mapping_template_id", nullable = false)
    private UUID mappingTemplateId;

    @Column(name = "table_name", nullable = false, unique = true)
    private String tableName;

    @Column(name = "ddl_applied", nullable = false, columnDefinition = "TEXT")
    private String ddlApplied;

    @Column(name = "dual_write_until")
    private OffsetDateTime dualWriteUntil;

    @Column(name = "status", nullable = false)
    private String status = "CREATING";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
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

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getDdlApplied() { return ddlApplied; }
    public void setDdlApplied(String ddlApplied) { this.ddlApplied = ddlApplied; }

    public OffsetDateTime getDualWriteUntil() { return dualWriteUntil; }
    public void setDualWriteUntil(OffsetDateTime dualWriteUntil) { this.dualWriteUntil = dualWriteUntil; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
