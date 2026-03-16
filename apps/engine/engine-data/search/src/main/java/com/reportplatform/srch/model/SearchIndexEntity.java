package com.reportplatform.srch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for search index storing FTS and vector data.
 */
@Entity
@Table(name = "search_index")
public class SearchIndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String orgId;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private UUID entityId;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "tsvector")
    private Object searchVector;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "vector")
    private float[] embedding;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public SearchIndexEntity() {}

    public SearchIndexEntity(UUID id, String orgId, String entityType, UUID entityId,
                             String title, String content, Object searchVector,
                             float[] embedding, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orgId = orgId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.title = title;
        this.content = content;
        this.searchVector = searchVector;
        this.embedding = embedding;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Object getSearchVector() { return searchVector; }
    public void setSearchVector(Object searchVector) { this.searchVector = searchVector; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String orgId;
        private String entityType;
        private UUID entityId;
        private String title;
        private String content;
        private float[] embedding;

        public Builder orgId(String orgId) { this.orgId = orgId; return this; }
        public Builder entityType(String entityType) { this.entityType = entityType; return this; }
        public Builder entityId(UUID entityId) { this.entityId = entityId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder embedding(float[] embedding) { this.embedding = embedding; return this; }

        public SearchIndexEntity build() {
            SearchIndexEntity entity = new SearchIndexEntity();
            entity.orgId = this.orgId;
            entity.entityType = this.entityType;
            entity.entityId = this.entityId;
            entity.title = this.title;
            entity.content = this.content;
            entity.embedding = this.embedding;
            return entity;
        }
    }
}
