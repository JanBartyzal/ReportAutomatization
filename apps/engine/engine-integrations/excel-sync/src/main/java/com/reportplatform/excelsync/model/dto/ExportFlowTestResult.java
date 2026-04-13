package com.reportplatform.excelsync.model.dto;

import java.util.List;
import java.util.Map;

public class ExportFlowTestResult {

    private List<String> headers;
    private List<Map<String, Object>> rows;
    private int totalRows;
    private boolean truncated;
    private String error;

    public ExportFlowTestResult() {
    }

    public ExportFlowTestResult(List<String> headers, List<Map<String, Object>> rows,
                                int totalRows, boolean truncated) {
        this.headers = headers;
        this.rows = rows;
        this.totalRows = totalRows;
        this.truncated = truncated;
    }

    public static ExportFlowTestResult error(String errorMessage) {
        ExportFlowTestResult result = new ExportFlowTestResult();
        result.setError(errorMessage);
        return result;
    }

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
