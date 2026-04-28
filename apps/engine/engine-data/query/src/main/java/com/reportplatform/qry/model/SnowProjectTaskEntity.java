package com.reportplatform.qry.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "snow_project_tasks")
public class SnowProjectTaskEntity {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "sys_id", length = 32)
    private String sysId;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "parent_sys_id", length = 32)
    private String parentSysId;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "is_milestone", nullable = false)
    private boolean milestone;

    @Column(name = "assigned_to_name", length = 255)
    private String assignedToName;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    // ---- Getters ----

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getProjectId() { return projectId; }
    public String getSysId() { return sysId; }
    public String getNumber() { return number; }
    public String getShortDescription() { return shortDescription; }
    public String getParentSysId() { return parentSysId; }
    public String getState() { return state; }
    public boolean isMilestone() { return milestone; }
    public String getAssignedToName() { return assignedToName; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getSyncedAt() { return syncedAt; }
}
