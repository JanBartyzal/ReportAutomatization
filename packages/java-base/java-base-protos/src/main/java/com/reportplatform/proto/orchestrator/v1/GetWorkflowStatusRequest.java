package com.reportplatform.proto.orchestrator.v1;

public class GetWorkflowStatusRequest {
    private String workflowId = "";

    public static GetWorkflowStatusRequest newBuilder() {
        return new GetWorkflowStatusRequest();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public GetWorkflowStatusRequest setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public GetWorkflowStatusRequest build() {
        return this;
    }
}
