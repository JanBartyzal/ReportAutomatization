package com.reportplatform.snow.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregated ITSM KPI summary for a connection or specific resolver group.
 * Returned by the /itsm-summary endpoint.
 */
public record ItsmSummaryDto(
        UUID connectionId,
        UUID resolverGroupId,
        String groupName,

        // Incident metrics
        long incidentOpenCount,
        long incidentResolvedCount,
        long incidentTotalCount,
        double incidentSlaBreachPct,
        double incidentAvgResolutionHours,
        long incidentCriticalOpenCount,

        // Request metrics
        long requestOpenCount,
        long requestClosedCount,
        long requestTotalCount,
        double requestAvgAgeHours,

        Instant dataAsOf
) {}
