package com.reportplatform.period.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CrossTypeComparisonRequest(
        @JsonAlias({"periodIds", "period_ids"}) List<UUID> periodIds,
        @JsonAlias({"holdingId", "holding_id"}) String holdingId,
        @JsonProperty("period1") UUID period1,
        @JsonProperty("period2") UUID period2
) {
    /**
     * Returns the effective period IDs list, building from period1/period2 if periodIds is empty.
     */
    public List<UUID> effectivePeriodIds() {
        if (periodIds != null && !periodIds.isEmpty()) {
            return periodIds;
        }
        List<UUID> ids = new ArrayList<>();
        if (period1 != null) ids.add(period1);
        if (period2 != null) ids.add(period2);
        return ids;
    }
}
