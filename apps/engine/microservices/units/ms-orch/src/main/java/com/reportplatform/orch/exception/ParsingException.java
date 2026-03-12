package com.reportplatform.orch.exception;

/**
 * Thrown when file parsing fails during the PARSING workflow step.
 */
public class ParsingException extends RuntimeException {

    private final String fileId;

    public ParsingException(String fileId, String message) {
        super(message);
        this.fileId = fileId;
    }

    public ParsingException(String fileId, String message, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}
