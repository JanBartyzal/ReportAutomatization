package com.reportplatform.snow.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "distribution_rules")
public class DistributionRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "report_template_id", nullable = false)
    private UUID reportTemplateId;

    /**
     * Stored as comma-separated string in JPA; the DB column is TEXT[].
     * Conversion happens via getter/setter helpers.
     */
    @Column(name = "recipients", nullable = false, columnDefinition = "TEXT")
    private String recipientsRaw = "";

    @Column(nullable = false, length = 10)
    private String format = "XLSX";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DistributionRuleEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public UUID getReportTemplateId() {
        return reportTemplateId;
    }

    public void setReportTemplateId(UUID reportTemplateId) {
        this.reportTemplateId = reportTemplateId;
    }

    public List<String> getRecipients() {
        if (recipientsRaw == null || recipientsRaw.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(recipientsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void setRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            this.recipientsRaw = "";
        } else {
            this.recipientsRaw = String.join(",", recipients);
        }
    }

    public String getRecipientsRaw() {
        return recipientsRaw;
    }

    public void setRecipientsRaw(String recipientsRaw) {
        this.recipientsRaw = recipientsRaw;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
