package com.reportplatform.proto.orchestrator.v1;

public class WorkflowStatusResponse {
    private String workflowId = "";
    private String fileId = "";
    private String status = "";
    private String startedAt = "";
    private String completedAt = "";
    private String stepsJson = "";

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final WorkflowStatusResponse response = new WorkflowStatusResponse();

        public Builder setWorkflowId(String workflowId) {
            response.workflowId = workflowId;
            return this;
        }

        public Builder setFileId(String fileId) {
            response.fileId = fileId;
            return this;
        }

        public Builder setStatus(String status) {
            response.status = status;
            return this;
        }

        public Builder setStartedAt(String startedAt) {
            response.startedAt = startedAt;
            return this;
        }

        public Builder setCompletedAt(String completedAt) {
            response.completedAt = completedAt;
            return this;
        }

        public Builder setStepsJson(String stepsJson) {
            response.stepsJson = stepsJson;
            return this;
        }

        public WorkflowStatusResponse build() {
            return response;
        }
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getStatus() {
        return status;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getStepsJson() {
        return stepsJson;
    }
}
