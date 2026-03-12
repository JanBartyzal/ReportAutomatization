package com.reportplatform.orch.config;

/**
 * States in the file processing workflow state machine.
 */
public enum WorkflowState {
    RECEIVED,
    SCANNING,
    PARSING,
    MAPPING,
    STORING,
    COMPLETED,
    FAILED
}
