package com.reportplatform.sink.tbl.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity for the promoted_tables_registry table.
 * Tracks dedicated tables that have been created from high-usage
 * JSONB mapping templates via the promotion workflow.
 */
@Entity
@Table(name = "promoted_tables_registry")
public class PromotedTableRegistryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mapping_template_id", nullable = false)
    private UUID mappingTemplateId;

    @Column(name = "table_name", nullable = false, unique = true, length = 255)
    private String tableName;

    @Column(name = "ddl_applied", nullable = false, columnDefinition = "TEXT")
    private String ddlApplied;

    @Column(name = "dual_write_until")
    private OffsetDateTime dualWriteUntil;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public PromotedTableRegistryEntity() {
    }

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

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMappingTemplateId() {
        return mappingTemplateId;
    }

    public void setMappingTemplateId(UUID mappingTemplateId) {
        this.mappingTemplateId = mappingTemplateId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDdlApplied() {
        return ddlApplied;
    }

    public void setDdlApplied(String ddlApplied) {
        this.ddlApplied = ddlApplied;
    }

    public OffsetDateTime getDualWriteUntil() {
        return dualWriteUntil;
    }

    public void setDualWriteUntil(OffsetDateTime dualWriteUntil) {
        this.dualWriteUntil = dualWriteUntil;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
