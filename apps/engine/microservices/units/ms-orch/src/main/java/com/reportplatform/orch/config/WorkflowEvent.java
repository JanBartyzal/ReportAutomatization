package com.reportplatform.orch.config;

/**
 * Events that trigger transitions in the file processing workflow state machine.
 */
public enum WorkflowEvent {
    FILE_RECEIVED,
    SCAN_COMPLETE,
    PARSE_COMPLETE,
    MAP_COMPLETE,
    STORE_COMPLETE,
    ERROR
}
