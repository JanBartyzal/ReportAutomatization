package com.reportplatform.proto.orchestrator.v1;

public class RetryWorkflowRequest {
    private String workflowId = "";

    public static RetryWorkflowRequest newBuilder() {
        return new RetryWorkflowRequest();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public RetryWorkflowRequest setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public RetryWorkflowRequest build() {
        return this;
    }
}
