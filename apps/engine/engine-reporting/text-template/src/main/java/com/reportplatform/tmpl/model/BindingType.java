package com.reportplatform.tmpl.model;

/** Defines how a placeholder binding resolves to content in the rendered output. */
public enum BindingType {
    /** Expands to a data table (rows from Named Query). */
    TABLE,
    /** Expands to a single scalar value (first column of first row). */
    SCALAR,
    /** Expands to a chart specification (type + data from Named Query). */
    CHART
}
