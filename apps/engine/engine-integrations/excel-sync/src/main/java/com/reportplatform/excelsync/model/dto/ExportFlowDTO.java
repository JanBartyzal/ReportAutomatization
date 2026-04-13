package com.reportplatform.excelsync.model.dto;

import com.reportplatform.excelsync.model.entity.*;
import java.time.Instant;
import java.util.UUID;

public class ExportFlowDTO {

    private UUID id;
    private String name;
    private String description;
    private String sqlQuery;
    private TargetType targetType;
    private String targetPath;
    private String targetSheet;
    private FileNaming fileNaming;
    private String customFileName;
    private TriggerType triggerType;
    private String triggerFilter;
    private String sharepointConfig;
    private boolean active;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private ExportFlowExecutionDTO lastExecution;

    public static ExportFlowDTO fromEntity(ExportFlowDefinitionEntity entity) {
        ExportFlowDTO dto = new ExportFlowDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSqlQuery(entity.getSqlQuery());
        dto.setTargetType(entity.getTargetType());
        dto.setTargetPath(entity.getTargetPath());
        dto.setTargetSheet(entity.getTargetSheet());
        dto.setFileNaming(entity.getFileNaming());
        dto.setCustomFileName(entity.getCustomFileName());
        dto.setTriggerType(entity.getTriggerType());
        dto.setTriggerFilter(entity.getTriggerFilter());
        dto.setSharepointConfig(entity.getSharepointConfig());
        dto.setActive(entity.isActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSqlQuery() { return sqlQuery; }
    public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

    public String getTargetSheet() { return targetSheet; }
    public void setTargetSheet(String targetSheet) { this.targetSheet = targetSheet; }

    public FileNaming getFileNaming() { return fileNaming; }
    public void setFileNaming(FileNaming fileNaming) { this.fileNaming = fileNaming; }

    public String getCustomFileName() { return customFileName; }
    public void setCustomFileName(String customFileName) { this.customFileName = customFileName; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public String getTriggerFilter() { return triggerFilter; }
    public void setTriggerFilter(String triggerFilter) { this.triggerFilter = triggerFilter; }

    public String getSharepointConfig() { return sharepointConfig; }
    public void setSharepointConfig(String sharepointConfig) { this.sharepointConfig = sharepointConfig; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public ExportFlowExecutionDTO getLastExecution() { return lastExecution; }
    public void setLastExecution(ExportFlowExecutionDTO lastExecution) { this.lastExecution = lastExecution; }
}
