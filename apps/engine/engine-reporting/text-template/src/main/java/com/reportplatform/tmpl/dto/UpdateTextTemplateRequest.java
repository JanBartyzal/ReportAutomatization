package com.reportplatform.tmpl.dto;

import jakarta.validation.constraints.Size;

/** Partial update request. All fields optional. System templates cannot be updated. */
public record UpdateTextTemplateRequest(
        @Size(max = 255) String name,
        String description,
        String templateType,
        String content,
        String outputFormats,
        String dataBindings,
        String scope,
        Boolean active
) {}
