package com.reportplatform.snow.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class CreateDistributionRuleRequest {

    @NotNull(message = "Schedule ID is required")
    private UUID scheduleId;

    @NotNull(message = "Report template ID is required")
    private UUID reportTemplateId;

    @NotEmpty(message = "At least one recipient is required")
    private List<String> recipients;

    @Size(max = 10, message = "Format must not exceed 10 characters")
    private String format = "XLSX";

    private boolean enabled = true;

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
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
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
}
