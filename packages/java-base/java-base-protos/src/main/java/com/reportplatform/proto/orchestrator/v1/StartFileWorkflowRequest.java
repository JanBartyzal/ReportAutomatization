package com.reportplatform.proto.orchestrator.v1;

public class StartFileWorkflowRequest {
    private String fileId = "";
    private String fileType = "";
    private String orgId = "";

    public static StartFileWorkflowRequest newBuilder() {
        return new StartFileWorkflowRequest();
    }

    public String getFileId() {
        return fileId;
    }

    public StartFileWorkflowRequest setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileType() {
        return fileType;
    }

    public StartFileWorkflowRequest setFileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    public String getOrgId() {
        return orgId;
    }

    public StartFileWorkflowRequest setOrgId(String orgId) {
        this.orgId = orgId;
        return this;
    }

    public StartFileWorkflowRequest build() {
        return this;
    }
}
