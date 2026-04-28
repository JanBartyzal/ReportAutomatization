package com.reportplatform.tmpl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating a new text template. */
public record CreateTextTemplateRequest(

        @NotBlank @Size(max = 255)
        String name,

        String description,

        /** MARKDOWN or HTML. Defaults to MARKDOWN. */
        String templateType,

        @NotBlank
        String content,

        /** JSON array of output formats: ["PPTX"], ["EXCEL"], ["PPTX","EXCEL"]. */
        String outputFormats,

        /**
         * JSON object with binding definitions.
         * Schema: {"bindings": [{"placeholder":"{{X}}","type":"TABLE","queryId":"uuid",...}]}
         */
        String dataBindings,

        /** CENTRAL or LOCAL. Defaults to CENTRAL. */
        String scope
) {}
