package com.reportplatform.qry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only entity mapping to the mv_file_summary materialized view.
 * Uses @Subselect to avoid Hibernate trying to create/validate the table.
 */
@Entity
@Immutable
@Subselect("SELECT * FROM mv_file_summary")
public class FileSummaryView {

    @Id
    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "filename")
    private String filename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "scan_status")
    private String scanStatus;

    @Column(name = "upload_purpose")
    private String uploadPurpose;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "latest_step")
    private String latestStep;

    @Column(name = "latest_status")
    private String latestStatus;

    @Column(name = "latest_step_at")
    private Instant latestStepAt;

    @Column(name = "table_count")
    private Long tableCount;

    @Column(name = "document_count")
    private Long documentCount;

    // Getters only (read-only view)

    public UUID getFileId() {
        return fileId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public String getUserId() {
        return userId;
    }

    public String getFilename() {
        return filename;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getScanStatus() {
        return scanStatus;
    }

    public String getUploadPurpose() {
        return uploadPurpose;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getLatestStep() {
        return latestStep;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public Instant getLatestStepAt() {
        return latestStepAt;
    }

    public Long getTableCount() {
        return tableCount;
    }

    public Long getDocumentCount() {
        return documentCount;
    }
}
