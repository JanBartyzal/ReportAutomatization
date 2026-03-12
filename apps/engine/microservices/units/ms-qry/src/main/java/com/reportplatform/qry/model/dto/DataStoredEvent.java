package com.reportplatform.qry.model.dto;

/**
 * Event received from Dapr Pub/Sub when new data is stored by a sink service.
 */
public record DataStoredEvent(
        String fileId,
        String orgId,
        String entityType,
        String action
) {
}
