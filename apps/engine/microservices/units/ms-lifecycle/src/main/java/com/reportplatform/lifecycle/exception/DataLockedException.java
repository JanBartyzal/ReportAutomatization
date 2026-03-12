package com.reportplatform.lifecycle.exception;

import java.util.UUID;

public class DataLockedException extends RuntimeException {
    public DataLockedException(UUID reportId) {
        super("Report data is locked (APPROVED): " + reportId);
    }
}
