package com.reportplatform.sink.tbl.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code storage_routing_config} table.
 * <p>
 * Each row defines which persistence backend ({@code POSTGRES} or {@code SPARK})
 * should receive table data for a given (org, source-type) combination.
 * A {@code NULL} in either discriminator column acts as a wildcard.
 * </p>
 *
 * <p>Specificity order used by {@link com.reportplatform.sink.tbl.service.StorageRoutingService}:
 * <ol>
 *   <li>org_id + source_type (most specific)</li>
 *   <li>org_id only</li>
 *   <li>source_type only</li>
 *   <li>global default (both NULL)</li>
 * </ol>
 * </p>
 */
@Entity(name = "StorageRoutingConfigEntity")
@Table(name = "storage_routing_config")
public class StorageRoutingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** {@code NULL} = rule applies to all organisations. */
    @Column(name = "org_id")
    private UUID orgId;

    /**
     * {@code NULL} = rule applies to all source types.
     * Known values: {@code EXCEL}, {@code PPTX}, {@code CSV}, {@code SERVICE_NOW}.
     */
    @Column(name = "source_type", length = 50)
    private String sourceType;

    /** Target backend: {@code POSTGRES} or {@code SPARK}. */
    @Column(name = "backend", nullable = false, length = 20)
    private String backend;

    /** Rule activates at this timestamp – allows future-dated cutover. */
    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (effectiveFrom == null) {
            effectiveFrom = OffsetDateTime.now();
        }
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getBackend() { return backend; }
    public void setBackend(String backend) { this.backend = backend; }

    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(OffsetDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
