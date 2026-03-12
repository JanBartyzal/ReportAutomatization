package com.reportplatform.admin.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the promotion_candidates table.
 * Represents a mapping template that has been identified as a candidate
 * for promotion from JSONB storage to a dedicated database table.
 */
@Entity
@Table(name = "promotion_candidates")
public class PromotionCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mapping_template_id", nullable = false)
    private UUID mappingTemplateId;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PromotionStatus status = PromotionStatus.CANDIDATE;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @Column(name = "proposed_table_name", nullable = false, length = 255)
    private String proposedTableName;

    @Column(name = "proposed_ddl", nullable = false, columnDefinition = "TEXT")
    private String proposedDdl;

    @Column(name = "proposed_indexes", columnDefinition = "TEXT")
    private String proposedIndexes;

    @Column(name = "column_type_analysis", columnDefinition = "jsonb")
    private String columnTypeAnalysis;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "final_ddl", columnDefinition = "TEXT")
    private String finalDdl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PromotionCandidateEntity() {
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters

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

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public void setStatus(PromotionStatus status) {
        this.status = status;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(long usageCount) {
        this.usageCount = usageCount;
    }

    public String getProposedTableName() {
        return proposedTableName;
    }

    public void setProposedTableName(String proposedTableName) {
        this.proposedTableName = proposedTableName;
    }

    public String getProposedDdl() {
        return proposedDdl;
    }

    public void setProposedDdl(String proposedDdl) {
        this.proposedDdl = proposedDdl;
    }

    public String getProposedIndexes() {
        return proposedIndexes;
    }

    public void setProposedIndexes(String proposedIndexes) {
        this.proposedIndexes = proposedIndexes;
    }

    public String getColumnTypeAnalysis() {
        return columnTypeAnalysis;
    }

    public void setColumnTypeAnalysis(String columnTypeAnalysis) {
        this.columnTypeAnalysis = columnTypeAnalysis;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getFinalDdl() {
        return finalDdl;
    }

    public void setFinalDdl(String finalDdl) {
        this.finalDdl = finalDdl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Lifecycle states for a promotion candidate.
     */
    public enum PromotionStatus {
        /** Automatically detected as a candidate based on usage threshold */
        CANDIDATE,
        /** Awaiting admin review */
        PENDING_REVIEW,
        /** Admin approved the promotion */
        APPROVED,
        /** Admin rejected the promotion */
        REJECTED,
        /** Dedicated table has been created */
        CREATED,
        /** Data migration from JSONB to dedicated table is in progress */
        MIGRATING,
        /** Promotion complete, dedicated table is actively used */
        ACTIVE
    }
}
