package com.reportplatform.excelsync.model.dto;

import com.reportplatform.excelsync.model.entity.ExecutionStatus;
import com.reportplatform.excelsync.model.entity.ExportFlowExecutionEntity;
import java.time.Instant;
import java.util.UUID;

public class ExportFlowExecutionDTO {

    private UUID id;
    private UUID flowId;
    private String triggerSource;
    private String triggerEventId;
    private ExecutionStatus status;
    private Integer rowsExported;
    private String targetPathUsed;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;

    public static ExportFlowExecutionDTO fromEntity(ExportFlowExecutionEntity entity) {
        ExportFlowExecutionDTO dto = new ExportFlowExecutionDTO();
        dto.setId(entity.getId());
        dto.setFlowId(entity.getFlowId());
        dto.setTriggerSource(entity.getTriggerSource());
        dto.setTriggerEventId(entity.getTriggerEventId());
        dto.setStatus(entity.getStatus());
        dto.setRowsExported(entity.getRowsExported());
        dto.setTargetPathUsed(entity.getTargetPathUsed());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getFlowId() { return flowId; }
    public void setFlowId(UUID flowId) { this.flowId = flowId; }

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
