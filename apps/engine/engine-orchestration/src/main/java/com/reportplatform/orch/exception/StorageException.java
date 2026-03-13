package com.reportplatform.orch.exception;

/**
 * Thrown when data storage fails during the STORING workflow step.
 */
public class StorageException extends RuntimeException {

    private final String fileId;

    public StorageException(String fileId, String message) {
        super(message);
        this.fileId = fileId;
    }

    public StorageException(String fileId, String message, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }
}
