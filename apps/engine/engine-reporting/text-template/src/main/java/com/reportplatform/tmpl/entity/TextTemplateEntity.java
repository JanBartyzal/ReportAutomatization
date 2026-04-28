package com.reportplatform.tmpl.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A unified text-based report template.
 * <p>
 * Templates are authored in Markdown or HTML and may contain {@code {{placeholder}}}
 * blocks bound to Named Queries (engine-data:query). The render service resolves
 * bindings, fetches data from ANY source, and calls the appropriate generator
 * (PPTX / Excel / HTML e-mail) to produce output.
 * <p>
 * System templates (is_system = true) are created by Flyway migrations, are
 * visible to all organisations, and cannot be modified or deleted by regular users.
 */
@Entity
@Table(name = "text_templates")
public class TextTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** NULL = system-wide template visible to all orgs. */
    @Column(name = "org_id")
    private UUID orgId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** MARKDOWN or HTML. */
    @Column(name = "template_type", nullable = false, length = 20)
    private String templateType = "MARKDOWN";

    /**
     * Template body with {{placeholder}} blocks.
     * Stored as TEXT; versioned in text_template_versions on each update.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * JSON array of supported output formats, e.g. ["PPTX","EXCEL"].
     * Allowed values: PPTX, EXCEL, HTML_EMAIL.
     */
    @Column(name = "output_formats", nullable = false, columnDefinition = "jsonb")
    private String outputFormats = "[\"PPTX\"]";

    /**
     * JSON object describing placeholder → Named Query bindings.
     * Schema: { "bindings": [ BindingEntry, ... ] }
     */
    @Column(name = "data_bindings", nullable = false, columnDefinition = "jsonb")
    private String dataBindings = "{\"bindings\":[]}";

    /** CENTRAL = available to all orgs; LOCAL = org-scoped. */
    @Column(nullable = false, length = 20)
    private String scope = "CENTRAL";

    /** System templates are immutable and cannot be deleted. */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TextTemplateEntity() {}

    public TextTemplateEntity(UUID orgId, String name, String description,
                               String templateType, String content,
                               String outputFormats, String dataBindings,
                               String scope, boolean system, String createdBy) {
        this.orgId = orgId;
        this.name = name;
        this.description = description;
        this.templateType = templateType != null ? templateType : "MARKDOWN";
        this.content = content;
        this.outputFormats = outputFormats != null ? outputFormats : "[\"PPTX\"]";
        this.dataBindings = dataBindings != null ? dataBindings : "{\"bindings\":[]}";
        this.scope = scope != null ? scope : "CENTRAL";
        this.system = system;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOutputFormats() { return outputFormats; }
    public void setOutputFormats(String outputFormats) { this.outputFormats = outputFormats; }
    public String getDataBindings() { return dataBindings; }
    public void setDataBindings(String dataBindings) { this.dataBindings = dataBindings; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
