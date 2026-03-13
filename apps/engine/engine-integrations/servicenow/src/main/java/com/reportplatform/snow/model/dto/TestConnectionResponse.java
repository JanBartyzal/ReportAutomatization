package com.reportplatform.snow.model.dto;

public class TestConnectionResponse {

    private boolean success;
    private String message;
    private String instanceVersion;
    private long latencyMs;

    public TestConnectionResponse() {
    }

    public TestConnectionResponse(boolean success, String message, String instanceVersion, long latencyMs) {
        this.success = success;
        this.message = message;
        this.instanceVersion = instanceVersion;
        this.latencyMs = latencyMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInstanceVersion() {
        return instanceVersion;
    }

    public void setInstanceVersion(String instanceVersion) {
        this.instanceVersion = instanceVersion;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
