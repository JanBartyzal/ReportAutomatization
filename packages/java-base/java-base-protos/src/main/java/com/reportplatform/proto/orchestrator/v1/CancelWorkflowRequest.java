package com.reportplatform.proto.orchestrator.v1;

public class CancelWorkflowRequest {
    private String workflowId = "";

    public static CancelWorkflowRequest newBuilder() {
        return new CancelWorkflowRequest();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public CancelWorkflowRequest setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public CancelWorkflowRequest build() {
        return this;
    }
}
