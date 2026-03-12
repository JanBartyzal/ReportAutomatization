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
@Table(name = "submission_checklists")
public class SubmissionChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "report_id", nullable = false, unique = true)
    private UUID reportId;

    @Column(name = "checklist_json", columnDefinition = "JSONB", nullable = false)
    private String checklistJson;

    @Column(name = "completed_pct", nullable = false)
    private int completedPct;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SubmissionChecklistEntity() {
        // JPA
    }

    public SubmissionChecklistEntity(UUID reportId, String checklistJson, int completedPct) {
        this.reportId = reportId;
        this.checklistJson = checklistJson;
        this.completedPct = completedPct;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public int getCompletedPct() { return completedPct; }
    public void setCompletedPct(int completedPct) { this.completedPct = completedPct; }

    public Instant getUpdatedAt() { return updatedAt; }
}
