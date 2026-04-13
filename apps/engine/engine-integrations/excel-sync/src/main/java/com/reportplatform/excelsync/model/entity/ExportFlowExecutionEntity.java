package com.reportplatform.excelsync.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_flow_executions")
public class ExportFlowExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flow_id", nullable = false)
    private UUID flowId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "trigger_source", length = 50)
    private String triggerSource;

    @Column(name = "trigger_event_id", length = 255)
    private String triggerEventId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "rows_exported")
    private Integer rowsExported;

    @Column(name = "target_path_used", length = 500)
    private String targetPathUsed;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ExportFlowExecutionEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getFlowId() { return flowId; }
    public void setFlowId(UUID flowId) { this.flowId = flowId; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }

    public String getTriggerEventId() { return triggerEventId; }
    public void setTriggerEventId(String triggerEventId) { this.triggerEventId = triggerEventId; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public Integer getRowsExported() { return rowsExported; }
    public void setRowsExported(Integer rowsExported) { this.rowsExported = rowsExported; }

    public String getTargetPathUsed() { return targetPathUsed; }
    public void setTargetPathUsed(String targetPathUsed) { this.targetPathUsed = targetPathUsed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
