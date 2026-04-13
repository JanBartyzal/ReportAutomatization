package com.reportplatform.excelsync.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_flow_definitions")
public class ExportFlowDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sql_query", nullable = false, columnDefinition = "text")
    private String sqlQuery;

    @Column(name = "target_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(name = "target_path", nullable = false, length = 500)
    private String targetPath;

    @Column(name = "target_sheet", nullable = false, length = 31)
    private String targetSheet;

    @Column(name = "file_naming", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FileNaming fileNaming = FileNaming.CUSTOM;

    @Column(name = "custom_file_name", length = 255)
    private String customFileName;

    @Column(name = "trigger_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType = TriggerType.MANUAL;

    @Column(name = "trigger_filter", columnDefinition = "jsonb")
    private String triggerFilter;

    @Column(name = "sharepoint_config", columnDefinition = "jsonb")
    private String sharepointConfig;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ExportFlowDefinitionEntity() {
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

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

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
