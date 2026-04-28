package com.reportplatform.qry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating a Named Query. */
public record CreateNamedQueryRequest(

        @NotBlank @Size(max = 255)
        String name,

        String description,

        @NotBlank
        String sqlQuery,

        /** JSON Schema string for parameter validation. Defaults to '{}'. */
        String paramsSchema,

        /** PLATFORM | SNOW_ITSM | SNOW_PROJECTS | FORMS | CUSTOM */
        String dataSourceHint
) {}
