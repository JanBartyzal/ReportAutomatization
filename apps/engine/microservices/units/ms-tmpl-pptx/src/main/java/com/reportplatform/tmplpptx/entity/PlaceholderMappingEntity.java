package com.reportplatform.tmplpptx.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "placeholder_mappings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "placeholder_key"}))
public class PlaceholderMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PptxTemplateEntity template;

    @Column(name = "placeholder_key", nullable = false)
    private String placeholderKey;

    @Column(name = "data_source_type", nullable = false, length = 30)
    private String dataSourceType;

    @Column(name = "data_source_ref", nullable = false, length = 512)
    private String dataSourceRef;

    @Column(name = "transform_expression", length = 1024)
    private String transformExpression;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaceholderMappingEntity() {}

    public PlaceholderMappingEntity(PptxTemplateEntity template, String placeholderKey,
                                    String dataSourceType, String dataSourceRef,
                                    String transformExpression, String createdBy) {
        this.template = template;
        this.placeholderKey = placeholderKey;
        this.dataSourceType = dataSourceType;
        this.dataSourceRef = dataSourceRef;
        this.transformExpression = transformExpression;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public PptxTemplateEntity getTemplate() { return template; }

    public String getPlaceholderKey() { return placeholderKey; }

    public String getDataSourceType() { return dataSourceType; }
    public void setDataSourceType(String dataSourceType) { this.dataSourceType = dataSourceType; }

    public String getDataSourceRef() { return dataSourceRef; }
    public void setDataSourceRef(String dataSourceRef) { this.dataSourceRef = dataSourceRef; }

    public String getTransformExpression() { return transformExpression; }
    public void setTransformExpression(String transformExpression) { this.transformExpression = transformExpression; }

    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
