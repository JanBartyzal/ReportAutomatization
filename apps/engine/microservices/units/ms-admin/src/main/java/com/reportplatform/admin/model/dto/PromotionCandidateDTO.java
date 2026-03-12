package com.reportplatform.admin.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for promotion candidate responses.
 */
public class PromotionCandidateDTO {

    private UUID id;
    private UUID mappingTemplateId;
    private UUID orgId;
    private String status;
    private long usageCount;
    private String proposedTableName;
    private String proposedDdl;
    private String proposedIndexes;
    private String columnTypeAnalysis;
    private String reviewedBy;
    private Instant reviewedAt;
    private String finalDdl;
    private Instant createdAt;
    private Instant updatedAt;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
