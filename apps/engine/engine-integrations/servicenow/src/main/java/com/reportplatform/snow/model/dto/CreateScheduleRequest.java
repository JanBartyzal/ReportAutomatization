package com.reportplatform.snow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateScheduleRequest {

    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @NotBlank(message = "Cron expression is required")
    @Size(max = 100, message = "Cron expression must not exceed 100 characters")
    private String cronExpression;

    private boolean enabled = true;

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
