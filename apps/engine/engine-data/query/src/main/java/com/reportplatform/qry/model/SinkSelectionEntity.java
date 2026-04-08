package com.reportplatform.qry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only entity for sink_selections table (FS25).
 */
@Entity(name = "QrySinkSelectionEntity")
@Immutable
@Table(name = "sink_selections")
public class SinkSelectionEntity {

    @Id
    @Column(name = "id", updatable = false)
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
    private boolean selected;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "selected_by")
    private String selectedBy;

    @Column(name = "selected_at")
    private Instant selectedAt;

    @Column(name = "note")
    private String note;

    public UUID getId() { return id; }
    public UUID getParsedTableId() { return parsedTableId; }
    public String getOrgId() { return orgId; }
    public String getPeriodId() { return periodId; }
    public String getReportType() { return reportType; }
    public boolean isSelected() { return selected; }
    public int getPriority() { return priority; }
    public String getSelectedBy() { return selectedBy; }
    public Instant getSelectedAt() { return selectedAt; }
    public String getNote() { return note; }
}
