package com.reportplatform.template.tmpl.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for the mapping_rules table.
 * Each rule belongs to a template and defines how a source column maps to a target.
 */
@Entity
@Table(name = "mapping_rules")
public class MappingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MappingTemplateEntity template;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "source_pattern", nullable = false, length = 500)
    private String sourcePattern;

    @Column(name = "target_column", nullable = false)
    private String targetColumn;

    @Column(name = "confidence")
    private double confidence = 1.0;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MappingTemplateEntity getTemplate() { return template; }
    public void setTemplate(MappingTemplateEntity template) { this.template = template; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getSourcePattern() { return sourcePattern; }
    public void setSourcePattern(String sourcePattern) { this.sourcePattern = sourcePattern; }

    public String getTargetColumn() { return targetColumn; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
