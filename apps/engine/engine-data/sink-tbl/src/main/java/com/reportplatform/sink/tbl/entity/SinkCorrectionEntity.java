package com.reportplatform.sink.tbl.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity for sink_corrections table.
 * Stores overlay corrections on immutable parsed_tables records (FS25).
 */
@Entity
@Table(name = "sink_corrections")
public class SinkCorrectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parsed_table_id", nullable = false)
    private UUID parsedTableId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(name = "col_index")
    private Integer colIndex;

    @Column(name = "original_value")
    private String originalValue;

    @Column(name = "corrected_value")
    private String correctedValue;

    @Column(name = "correction_type", nullable = false)
    private String correctionType;

    @Column(name = "corrected_by", nullable = false)
    private String correctedBy;

    @Column(name = "corrected_at", nullable = false)
    private OffsetDateTime correctedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (correctedAt == null) {
            correctedAt = OffsetDateTime.now();
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

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public Integer getColIndex() {
        return colIndex;
    }

    public void setColIndex(Integer colIndex) {
        this.colIndex = colIndex;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getCorrectedValue() {
        return correctedValue;
    }

    public void setCorrectedValue(String correctedValue) {
        this.correctedValue = correctedValue;
    }

    public String getCorrectionType() {
        return correctionType;
    }

    public void setCorrectionType(String correctionType) {
        this.correctionType = correctionType;
    }

    public String getCorrectedBy() {
        return correctedBy;
    }

    public void setCorrectedBy(String correctedBy) {
        this.correctedBy = correctedBy;
    }

    public OffsetDateTime getCorrectedAt() {
        return correctedAt;
    }

    public void setCorrectedAt(OffsetDateTime correctedAt) {
        this.correctedAt = correctedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
