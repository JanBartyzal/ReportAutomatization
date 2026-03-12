package com.reportplatform.lifecycle.exception;

public class ChecklistIncompleteException extends RuntimeException {
    public ChecklistIncompleteException(int completedPct) {
        super("Submission checklist incomplete: " + completedPct + "% (100% required)");
    }
}
