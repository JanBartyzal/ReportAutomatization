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
 * JPA entity tracking the history and current status of a file processing workflow.
 */
@Entity
@Table(name = "workflow_history")
public class WorkflowHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "workflow_id", nullable = false, unique = true)
    private String workflowId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "steps_json", columnDefinition = "JSONB")
    private String stepsJson;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    protected WorkflowHistoryEntity() {
        // JPA
    }

    public WorkflowHistoryEntity(String fileId, String workflowId, String status, String orgId) {
        this.fileId = fileId;
        this.workflowId = workflowId;
        this.status = status;
        this.startedAt = Instant.now();
        this.orgId = orgId;
    }

    public UUID getId() { return id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public void markCompleted(String finalStatus) {
        this.status = finalStatus;
        this.completedAt = Instant.now();
    }
}
