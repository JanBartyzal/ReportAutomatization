package com.reportplatform.period.model;

import com.reportplatform.period.config.PeriodState;
import com.reportplatform.period.config.PeriodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "periods")
public class PeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "period_code", nullable = false, unique = true)
    private String periodCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "submission_deadline", nullable = false)
    private Instant submissionDeadline;

    @Column(name = "review_deadline", nullable = false)
    private Instant reviewDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PeriodState status = PeriodState.OPEN;

    @Column(name = "holding_id", nullable = false)
    private String holdingId;

    @Column(name = "cloned_from_id")
    private UUID clonedFromId;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PeriodEntity() {
        // JPA
    }

    public PeriodEntity(String name, PeriodType periodType, String periodCode,
                        LocalDate startDate, LocalDate endDate,
                        Instant submissionDeadline, Instant reviewDeadline,
                        String holdingId, String createdBy) {
        this.name = name;
        this.periodType = periodType;
        this.periodCode = periodCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.submissionDeadline = submissionDeadline;
        this.reviewDeadline = reviewDeadline;
        this.holdingId = holdingId;
        this.createdBy = createdBy;
        this.status = PeriodState.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }

    public String getPeriodCode() { return periodCode; }
    public void setPeriodCode(String periodCode) { this.periodCode = periodCode; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Instant getSubmissionDeadline() { return submissionDeadline; }
    public void setSubmissionDeadline(Instant submissionDeadline) { this.submissionDeadline = submissionDeadline; }

    public Instant getReviewDeadline() { return reviewDeadline; }
    public void setReviewDeadline(Instant reviewDeadline) { this.reviewDeadline = reviewDeadline; }

    public PeriodState getStatus() { return status; }
    public void setStatus(PeriodState status) { this.status = status; }

    public String getHoldingId() { return holdingId; }
    public void setHoldingId(String holdingId) { this.holdingId = holdingId; }

    public UUID getClonedFromId() { return clonedFromId; }
    public void setClonedFromId(UUID clonedFromId) { this.clonedFromId = clonedFromId; }

    public String getCreatedBy() { return createdBy; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
