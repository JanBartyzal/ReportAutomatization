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
 * Read-only entity mapping to the parsed_tables table (owned by ms-sink-tbl).
 */
@Entity
@Immutable
@Table(name = "parsed_tables")
public class ParsedTableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "scope", nullable = false)
    private String scope = "CENTRAL";

    @Column(name = "source_sheet")
    private String sourceSheet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Object headers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rows", columnDefinition = "jsonb")
    private Object rows;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Object metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    // Getters only (read-only entity)

    public UUID getId() {
        return id;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getScope() {
        return scope;
    }

    public String getSourceSheet() {
        return sourceSheet;
    }

    public Object getHeaders() {
        return headers;
    }

    public Object getRows() {
        return rows;
    }

    public Object getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
