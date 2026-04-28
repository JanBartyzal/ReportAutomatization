package com.reportplatform.tmpl.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request to render a text template with runtime parameters.
 * <p>
 * {@code params} are substituted into binding entries that use
 * {@code {{input.paramName}}} references.
 */
public record RenderRequest(

        /** Desired output format. Must be one of the template's supported outputFormats. */
        @NotBlank
        String outputFormat,

        /**
         * Runtime parameter values used to resolve binding params.
         * Example: {"groupId": "abc-123", "period": "Q1-2026"}
         */
        Map<String, String> params
) {
    public RenderRequest {
        if (params == null) params = Map.of();
    }
}
