package com.reportplatform.qry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only entity mapping to the processing_logs table (owned by ms-sink-log).
 */
@Entity(name = "QryProcessingLogEntity")
@Immutable
@Table(name = "processing_logs")
public class ProcessingLogEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "workflow_id")
    private String workflowId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_detail")
    private String errorDetail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Object metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    // Getters only (read-only entity)

    public UUID getId() {
        return id;
    }

    public String getFileId() {
        return fileId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getStepName() {
        return stepName;
    }

    public String getStatus() {
        return status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public Object getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
