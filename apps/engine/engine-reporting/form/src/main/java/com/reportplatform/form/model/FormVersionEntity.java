package com.reportplatform.form.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "form_versions")
public class FormVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_def", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> schemaDef;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FormVersionEntity() {
    }

    public FormVersionEntity(UUID formId, int versionNumber, Map<String, Object> schemaDef, String createdBy) {
        this.formId = formId;
        this.versionNumber = versionNumber;
        this.schemaDef = schemaDef;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getFormId() { return formId; }
    public void setFormId(UUID formId) { this.formId = formId; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public Map<String, Object> getSchemaDef() { return schemaDef; }
    public void setSchemaDef(Map<String, Object> schemaDef) { this.schemaDef = schemaDef; }

    public String getCreatedBy() { return createdBy; }

    public Instant getCreatedAt() { return createdAt; }
}
