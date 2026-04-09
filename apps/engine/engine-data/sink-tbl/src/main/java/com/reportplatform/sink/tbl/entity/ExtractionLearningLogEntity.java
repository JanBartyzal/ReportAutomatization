package com.reportplatform.sink.tbl.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity for extraction_learning_log table.
 * Stores extraction error feedback for AI learning pipeline (FS25).
 */
@Entity
@Table(name = "extraction_learning_log")
public class ExtractionLearningLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "parsed_table_id")
    private UUID parsedTableId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "slide_index")
    private Integer slideIndex;

    @Column(name = "error_category")
    private String errorCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_snippet", columnDefinition = "jsonb")
    private String originalSnippet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "corrected_snippet", columnDefinition = "jsonb")
    private String correctedSnippet;

    @Column(name = "confidence_score")
    private Float confidenceScore;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "applied", nullable = false)
    private boolean applied = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public UUID getParsedTableId() {
        return parsedTableId;
    }

    public void setParsedTableId(UUID parsedTableId) {
        this.parsedTableId = parsedTableId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getSlideIndex() {
        return slideIndex;
    }

    public void setSlideIndex(Integer slideIndex) {
        this.slideIndex = slideIndex;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public String getOriginalSnippet() {
        return originalSnippet;
    }

    public void setOriginalSnippet(String originalSnippet) {
        this.originalSnippet = originalSnippet;
    }

    public String getCorrectedSnippet() {
        return correctedSnippet;
    }

    public void setCorrectedSnippet(String correctedSnippet) {
        this.correctedSnippet = correctedSnippet;
    }

    public Float getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Float confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }
}
