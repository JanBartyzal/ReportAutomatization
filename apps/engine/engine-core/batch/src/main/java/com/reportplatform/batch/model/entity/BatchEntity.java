package com.reportplatform.batch.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batches")
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String period;

    @Column(name = "period_id")
    private UUID periodId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "holding_id", nullable = false)
    private UUID holdingId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.OPEN;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    public BatchEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public UUID getPeriodId() {
        return periodId;
    }

    public void setPeriodId(UUID periodId) {
        this.periodId = periodId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(UUID holdingId) {
        this.holdingId = holdingId;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public enum BatchStatus {
        OPEN,
        COLLECTING,
        CLOSED
    }
}
