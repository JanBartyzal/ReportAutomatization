package com.reportplatform.snow.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "distribution_history")
public class DistributionHistoryEntity {

    public enum DistributionStatus {
        PENDING, SENT, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    /**
     * Stored as comma-separated string in JPA; the DB column is TEXT[].
     */
    @Column(name = "recipients", nullable = false, columnDefinition = "TEXT")
    private String recipientsRaw = "";

    @Column(name = "report_blob_url", columnDefinition = "TEXT")
    private String reportBlobUrl;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DistributionStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DistributionHistoryEntity() {
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

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
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

    public String getReportBlobUrl() {
        return reportBlobUrl;
    }

    public void setReportBlobUrl(String reportBlobUrl) {
        this.reportBlobUrl = reportBlobUrl;
    }

    public DistributionStatus getStatus() {
        return status;
    }

    public void setStatus(DistributionStatus status) {
        this.status = status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
