package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Result of executing a Named Query. */
public record NamedQueryResultDto(
        UUID queryId,
        String queryName,
        List<Map<String, Object>> rows,
        int totalCount,
        Instant executedAt
) {}
