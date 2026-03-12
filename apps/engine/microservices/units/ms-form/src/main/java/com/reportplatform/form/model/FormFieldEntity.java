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
@Table(name = "form_fields")
public class FormFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "form_version_id", nullable = false)
    private UUID formVersionId;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType;

    @Column(name = "label", nullable = false, length = 500)
    private String label;

    @Column(name = "section")
    private String section;

    @Column(name = "section_description", columnDefinition = "TEXT")
    private String sectionDescription;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "required", nullable = false)
    private boolean required;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FormFieldEntity() {
    }

    public FormFieldEntity(UUID formVersionId, String fieldKey, String fieldType, String label,
                           String section, String sectionDescription, int sortOrder,
                           boolean required, Map<String, Object> properties) {
        this.formVersionId = formVersionId;
        this.fieldKey = fieldKey;
        this.fieldType = fieldType;
        this.label = label;
        this.section = section;
        this.sectionDescription = sectionDescription;
        this.sortOrder = sortOrder;
        this.required = required;
        this.properties = properties;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getFormVersionId() { return formVersionId; }
    public void setFormVersionId(UUID formVersionId) { this.formVersionId = formVersionId; }

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getSectionDescription() { return sectionDescription; }
    public void setSectionDescription(String sectionDescription) { this.sectionDescription = sectionDescription; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public Instant getCreatedAt() { return createdAt; }
}
