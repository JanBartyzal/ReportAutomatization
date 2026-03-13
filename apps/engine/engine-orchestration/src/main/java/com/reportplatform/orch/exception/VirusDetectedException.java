package com.reportplatform.orch.exception;

/**
 * Thrown when a virus is detected during the SCANNING workflow step.
 */
public class VirusDetectedException extends RuntimeException {

    private final String fileId;

    public VirusDetectedException(String fileId, String message) {
        super(message);
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}
