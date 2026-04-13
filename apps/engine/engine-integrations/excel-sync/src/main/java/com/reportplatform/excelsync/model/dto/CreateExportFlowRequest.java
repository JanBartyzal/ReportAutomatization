package com.reportplatform.excelsync.model.dto;

import com.reportplatform.excelsync.model.entity.FileNaming;
import com.reportplatform.excelsync.model.entity.TargetType;
import com.reportplatform.excelsync.model.entity.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateExportFlowRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank
    private String sqlQuery;

    @NotNull
    private TargetType targetType;

    @NotBlank
    @Size(max = 500)
    private String targetPath;

    @NotBlank
    @Size(max = 31)
    private String targetSheet;

    private FileNaming fileNaming = FileNaming.CUSTOM;

    @Size(max = 255)
    private String customFileName;

    private TriggerType triggerType = TriggerType.MANUAL;

    private String triggerFilter;

    private String sharepointConfig;

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
}
