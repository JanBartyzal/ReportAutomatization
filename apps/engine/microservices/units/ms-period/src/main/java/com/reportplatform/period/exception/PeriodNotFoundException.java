package com.reportplatform.period.exception;

import java.util.UUID;

public class PeriodNotFoundException extends RuntimeException {
    public PeriodNotFoundException(UUID periodId) {
        super("Period not found: " + periodId);
    }
}
