package com.reportplatform.orch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a failed job in the file processing pipeline.
 */
@Entity
@Table(name = "failed_jobs")
public class FailedJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "error_type", nullable = false)
    private String errorType;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    protected FailedJobEntity() {
        // JPA
    }

    public FailedJobEntity(String fileId, String workflowId, String errorType,
                           String errorDetail, String orgId) {
        this.fileId = fileId;
        this.workflowId = workflowId;
        this.errorType = errorType;
        this.errorDetail = errorDetail;
        this.failedAt = Instant.now();
        this.retryCount = 0;
        this.orgId = orgId;
    }

    public UUID getId() { return id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
