package com.reportplatform.qry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only entity for sink_corrections table (FS25).
 */
@Entity(name = "QrySinkCorrectionEntity")
@Immutable
@Table(name = "sink_corrections")
public class SinkCorrectionEntity {

    @Id
    @Column(name = "id", updatable = false)
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
    private Instant correctedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Object metadata;

    public UUID getId() { return id; }
    public UUID getParsedTableId() { return parsedTableId; }
    public String getOrgId() { return orgId; }
    public Integer getRowIndex() { return rowIndex; }
    public Integer getColIndex() { return colIndex; }
    public String getOriginalValue() { return originalValue; }
    public String getCorrectedValue() { return correctedValue; }
    public String getCorrectionType() { return correctionType; }
    public String getCorrectedBy() { return correctedBy; }
    public Instant getCorrectedAt() { return correctedAt; }
    public Object getMetadata() { return metadata; }
}
