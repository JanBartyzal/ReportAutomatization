package com.reportplatform.period.exception;

import java.util.UUID;

public class DeadlinePassedException extends RuntimeException {
    public DeadlinePassedException(UUID periodId) {
        super("Submission deadline has passed for period: " + periodId);
    }
}
