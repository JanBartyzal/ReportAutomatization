package com.reportplatform.snow.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Configures which ServiceNow assignment_groups to monitor for ITSM data.
 * One connection can have many resolver groups; each group defines which
 * data types (INCIDENT, REQUEST, TASK) to fetch.
 */
@Entity
@Table(name = "snow_resolver_groups")
public class ResolverGroupConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    /** ServiceNow sys_id of the assignment_group record. */
    @Column(name = "group_sys_id", nullable = false, length = 32)
    private String groupSysId;

    /** Display name of the group (cached from SN for convenience). */
    @Column(name = "group_name", nullable = false, length = 255)
    private String groupName;

    /**
     * JSON array of data types to sync for this group.
     * Valid values: "INCIDENT", "REQUEST", "TASK"
     * Example: ["INCIDENT","REQUEST"]
     */
    @Column(name = "data_types", nullable = false, columnDefinition = "jsonb")
    private String dataTypes = "[\"INCIDENT\"]";

    @Column(name = "sync_enabled", nullable = false)
    private boolean syncEnabled = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ResolverGroupConfigEntity() {}

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = connectionId; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getGroupSysId() { return groupSysId; }
    public void setGroupSysId(String groupSysId) { this.groupSysId = groupSysId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getDataTypes() { return dataTypes; }
    public void setDataTypes(String dataTypes) { this.dataTypes = dataTypes; }

    public boolean isSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
