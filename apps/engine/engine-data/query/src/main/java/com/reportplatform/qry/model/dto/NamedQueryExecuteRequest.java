package com.reportplatform.qry.model.dto;

import java.util.Map;

/** Request body for executing a Named Query with runtime parameters. */
public record NamedQueryExecuteRequest(
        /** Runtime parameter values keyed by parameter name (e.g. {"groupId": "abc-123"}). */
        Map<String, String> params,
        /** Maximum rows to return. Defaults to 500; hard-capped at 5000. */
        Integer limit
) {
    public NamedQueryExecuteRequest {
        if (params == null) params = Map.of();
        if (limit == null) limit = 500;
        if (limit > 5000) limit = 5000;
    }
}
