package com.reportplatform.proto.orchestrator.v1;

import java.util.ArrayList;
import java.util.List;

public class ListFailedJobsResponse {
    private List<FailedJob> jobs = new ArrayList<>();
    private int totalCount = 0;
    private int page = 0;
    private int size = 20;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final ListFailedJobsResponse response = new ListFailedJobsResponse();

        public Builder setJobs(List<FailedJob> jobs) {
            response.jobs = jobs;
            return this;
        }

        public Builder addJobs(FailedJob job) {
            response.jobs.add(job);
            return this;
        }

        public Builder setTotalCount(int totalCount) {
            response.totalCount = totalCount;
            return this;
        }

        public Builder setPage(int page) {
            response.page = page;
            return this;
        }

        public Builder setSize(int size) {
            response.size = size;
            return this;
        }

        public ListFailedJobsResponse build() {
            return response;
        }
    }

    public List<FailedJob> getJobs() {
        return jobs;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public static class FailedJob {
        private String jobId = "";
        private String fileId = "";
        private String workflowId = "";
        private String errorType = "";
        private String errorDetail = "";
        private String failedAt = "";
        private int retryCount = 0;

        public static FailedJob.Builder newBuilder() {
            return new FailedJob.Builder();
        }

        public static class Builder {
            private final FailedJob job = new FailedJob();

            public Builder setJobId(String jobId) {
                job.jobId = jobId;
                return this;
            }

            public Builder setFileId(String fileId) {
                job.fileId = fileId;
                return this;
            }

            public Builder setWorkflowId(String workflowId) {
                job.workflowId = workflowId;
                return this;
            }

            public Builder setErrorType(String errorType) {
                job.errorType = errorType;
                return this;
            }

            public Builder setErrorDetail(String errorDetail) {
                job.errorDetail = errorDetail;
                return this;
            }

            public Builder setFailedAt(String failedAt) {
                job.failedAt = failedAt;
                return this;
            }

            public Builder setRetryCount(int retryCount) {
                job.retryCount = retryCount;
                return this;
            }

            public FailedJob build() {
                return job;
            }
        }

        public String getJobId() {
            return jobId;
        }

        public String getFileId() {
            return fileId;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public String getErrorType() {
            return errorType;
        }

        public String getErrorDetail() {
            return errorDetail;
        }

        public String getFailedAt() {
            return failedAt;
        }

        public int getRetryCount() {
            return retryCount;
        }
    }
}
