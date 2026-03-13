package com.reportplatform.proto.orchestrator.v1;

public class StartFileWorkflowResponse {
    private String workflowId = "";
    private String status = "";

    public static StartFileWorkflowResponse newBuilder() {
        return new StartFileWorkflowResponse();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public StartFileWorkflowResponse setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public StartFileWorkflowResponse setStatus(String status) {
        this.status = status;
        return this;
    }

    public StartFileWorkflowResponse build() {
        return this;
    }
}
