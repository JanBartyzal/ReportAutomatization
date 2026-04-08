package com.reportplatform.sink.tbl.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity for sink_selections table.
 * Tracks which parsed tables are selected for final report/dashboard output (FS25).
 */
@Entity
@Table(name = "sink_selections",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_sink_selection_table_period_type",
           columnNames = {"parsed_table_id", "period_id", "report_type"}
       ))
public class SinkSelectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parsed_table_id", nullable = false)
    private UUID parsedTableId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "period_id")
    private String periodId;

    @Column(name = "report_type")
    private String reportType;

    @Column(name = "selected", nullable = false)
    private boolean selected = true;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "selected_by")
    private String selectedBy;

    @Column(name = "selected_at")
    private OffsetDateTime selectedAt;

    @Column(name = "note")
    private String note;

    @PrePersist
    protected void onCreate() {
        if (selectedAt == null) {
            selectedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParsedTableId() {
        return parsedTableId;
    }

    public void setParsedTableId(UUID parsedTableId) {
        this.parsedTableId = parsedTableId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSelectedBy() {
        return selectedBy;
    }

    public void setSelectedBy(String selectedBy) {
        this.selectedBy = selectedBy;
    }

    public OffsetDateTime getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(OffsetDateTime selectedAt) {
        this.selectedAt = selectedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
