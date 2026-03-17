package com.reportplatform.admin.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the health_service_registry table.
 * Stores service definitions used by the health dashboard to probe actuator endpoints.
 */
@Entity
@Table(name = "health_service_registry")
public class HealthServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "service_id", nullable = false, unique = true, length = 100)
    private String serviceId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "health_url", nullable = false, length = 500)
    private String healthUrl;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HealthServiceEntity() {
        // JPA
    }

    public HealthServiceEntity(String serviceId, String displayName, String healthUrl, int sortOrder) {
        this.serviceId = serviceId;
        this.displayName = displayName;
        this.healthUrl = healthUrl;
        this.sortOrder = sortOrder;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getHealthUrl() { return healthUrl; }
    public void setHealthUrl(String healthUrl) { this.healthUrl = healthUrl; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
