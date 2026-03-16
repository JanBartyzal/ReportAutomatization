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
 * Read-only entity mapping to the documents table (owned by ms-sink-doc).
 */
@Entity(name = "QryDocumentEntity")
@Immutable
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb")
    private Object content;

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

    public String getDocumentType() {
        return documentType;
    }

    public Object getContent() {
        return content;
    }

    public Object getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
