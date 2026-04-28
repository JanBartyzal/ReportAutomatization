package com.reportplatform.qry.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a saved, parameterized SQL query in the Named Query Catalog.
 * <p>
 * Named queries are DATA-SOURCE AGNOSTIC – they can reference any table in the
 * platform database (parsed_tables, documents, snow_incidents, snow_projects,
 * form_responses, etc.).  They serve as binding targets for Text Templates
 * (engine-reporting:text-template) and as a standalone data-access API.
 * <p>
 * System queries (is_system = true) are created by migrations and visible to
 * all organisations. Org-scoped queries are protected by RLS policies.
 */
@Entity
@Table(name = "named_queries")
public class NamedQueryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** NULL means system-wide (visible to all orgs). */
    @Column(name = "org_id")
    private UUID orgId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Parameterized SQL using :paramName syntax (JPA named parameters). */
    @Column(name = "sql_query", nullable = false, columnDefinition = "TEXT")
    private String sqlQuery;

    /**
     * JSON Schema defining accepted parameters.
     * Example: {"properties": {"groupId": {"type": "string"}}, "required": ["groupId"]}
     */
    @Column(name = "params_schema", nullable = false, columnDefinition = "jsonb")
    private String paramsSchema = "{}";

    /**
     * Human-readable hint about the primary data source.
     * Allowed values: PLATFORM, SNOW_ITSM, SNOW_PROJECTS, FORMS, CUSTOM.
     */
    @Column(name = "data_source_hint", nullable = false, length = 50)
    private String dataSourceHint = "PLATFORM";

    /** System queries are read-only for non-admin users and cannot be deleted. */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NamedQueryEntity() {}

    public NamedQueryEntity(UUID orgId, String name, String description,
                             String sqlQuery, String paramsSchema,
                             String dataSourceHint, boolean system, String createdBy) {
        this.orgId = orgId;
        this.name = name;
        this.description = description;
        this.sqlQuery = sqlQuery;
        this.paramsSchema = paramsSchema != null ? paramsSchema : "{}";
        this.dataSourceHint = dataSourceHint != null ? dataSourceHint : "PLATFORM";
        this.system = system;
        this.createdBy = createdBy;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSqlQuery() { return sqlQuery; }
    public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }

    public String getParamsSchema() { return paramsSchema; }
    public void setParamsSchema(String paramsSchema) { this.paramsSchema = paramsSchema; }

    public String getDataSourceHint() { return dataSourceHint; }
    public void setDataSourceHint(String dataSourceHint) { this.dataSourceHint = dataSourceHint; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedBy() { return createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
