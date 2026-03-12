package com.reportplatform.lifecycle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_status_history")
public class ReportStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReportStatusHistoryEntity() {
        // JPA
    }

    public ReportStatusHistoryEntity(UUID reportId, String fromStatus, String toStatus,
                                     String userId, String comment) {
        this.reportId = reportId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.userId = userId;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getReportId() { return reportId; }

    public String getFromStatus() { return fromStatus; }

    public String getToStatus() { return toStatus; }

    public String getUserId() { return userId; }

    public String getComment() { return comment; }

    public Instant getCreatedAt() { return createdAt; }
}
