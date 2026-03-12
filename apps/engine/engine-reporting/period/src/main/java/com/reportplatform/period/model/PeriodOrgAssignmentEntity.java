package com.reportplatform.period.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "period_org_assignments")
public class PeriodOrgAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected PeriodOrgAssignmentEntity() {
        // JPA
    }

    public PeriodOrgAssignmentEntity(UUID periodId, String orgId) {
        this.periodId = periodId;
        this.orgId = orgId;
        this.assignedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getPeriodId() { return periodId; }

    public String getOrgId() { return orgId; }

    public Instant getAssignedAt() { return assignedAt; }
}
