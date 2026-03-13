package com.reportplatform.proto.orchestrator.v1;

public class ListFailedJobsRequest {
    private String orgId = "";
    private int page = 0;
    private int size = 20;

    public static ListFailedJobsRequest newBuilder() {
        return new ListFailedJobsRequest();
    }

    public String getOrgId() {
        return orgId;
    }

    public ListFailedJobsRequest setOrgId(String orgId) {
        this.orgId = orgId;
        return this;
    }

    public int getPage() {
        return page;
    }

    public ListFailedJobsRequest setPage(int page) {
        this.page = page;
        return this;
    }

    public int getSize() {
        return size;
    }

    public ListFailedJobsRequest setSize(int size) {
        this.size = size;
        return this;
    }

    public ListFailedJobsRequest build() {
        return this;
    }
}
