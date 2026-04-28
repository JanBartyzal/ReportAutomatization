package com.reportplatform.tmpl.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * A single data binding entry within a {@code TextTemplate.dataBindings.bindings[]} array.
 * <p>
 * Example JSON:
 * <pre>{@code
 * {
 *   "placeholder": "{{INCIDENTS_TABLE}}",
 *   "type": "TABLE",
 *   "queryId": "d4e5f6a7-...",
 *   "params": { "groupId": "{{input.groupId}}" },
 *   "chartType": "PIE",
 *   "label": "Open Incidents"
 * }
 * }</pre>
 * <p>
 * The {@code params} map supports literal values and {@code {{input.paramName}}} references
 * resolved from the render-time input parameters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BindingEntry(
        /** The placeholder string in the template content, e.g. {@code {{INCIDENTS_TABLE}}}. */
        String placeholder,

        /** How the resolved data is rendered (TABLE, SCALAR, CHART). */
        BindingType type,

        /** UUID of the Named Query to execute for this binding. */
        String queryId,

        /**
         * Parameter values for the Named Query.
         * Supports {@code {{input.paramName}}} references resolved from render-time params.
         */
        Map<String, String> params,

        /** Required when type=CHART. Allowed: PIE, BAR, LINE, COLUMN. */
        String chartType,

        /** Human-readable label used in the generated output (e.g. chart title). */
        String label
) {}
