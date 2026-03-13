package com.reportplatform.proto.orchestrator.v1;

public class CancelWorkflowResponse {
    private boolean success = false;
    private String message = "";

    public static CancelWorkflowResponse newBuilder() {
        return new CancelWorkflowResponse();
    }

    public boolean isSuccess() {
        return success;
    }

    public CancelWorkflowResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public CancelWorkflowResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public CancelWorkflowResponse build() {
        return this;
    }
}
