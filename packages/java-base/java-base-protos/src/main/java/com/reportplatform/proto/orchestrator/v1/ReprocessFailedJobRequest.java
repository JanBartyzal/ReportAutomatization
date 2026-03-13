package com.reportplatform.proto.orchestrator.v1;

public class ReprocessFailedJobRequest {
    private String jobId = "";

    public static ReprocessFailedJobRequest newBuilder() {
        return new ReprocessFailedJobRequest();
    }

    public String getJobId() {
        return jobId;
    }

    public ReprocessFailedJobRequest setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public ReprocessFailedJobRequest build() {
        return this;
    }
}
