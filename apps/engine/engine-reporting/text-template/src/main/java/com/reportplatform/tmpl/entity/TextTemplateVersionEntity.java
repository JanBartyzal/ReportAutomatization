package com.reportplatform.tmpl.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Immutable snapshot of a TextTemplate at a specific version. */
@Entity
@Table(name = "text_template_versions")
public class TextTemplateVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "data_bindings", nullable = false, columnDefinition = "jsonb")
    private String dataBindings;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TextTemplateVersionEntity() {}

    public TextTemplateVersionEntity(UUID templateId, int version,
                                      String content, String dataBindings, String createdBy) {
        this.templateId = templateId;
        this.version = version;
        this.content = content;
        this.dataBindings = dataBindings;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public UUID getTemplateId() { return templateId; }
    public int getVersion() { return version; }
    public String getContent() { return content; }
    public String getDataBindings() { return dataBindings; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
