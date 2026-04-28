package com.reportplatform.qry.model.dto;

import jakarta.validation.constraints.Size;

/** Request body for updating a Named Query. All fields optional (partial update). */
public record UpdateNamedQueryRequest(
        @Size(max = 255) String name,
        String description,
        String sqlQuery,
        String paramsSchema,
        String dataSourceHint,
        Boolean active
) {}
