package com.reportplatform.form.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record FormFieldDefinition(
        @NotBlank String fieldKey,
        @NotBlank String fieldType,
        @NotBlank String label,
        String section,
        String sectionDescription,
        int sortOrder,
        boolean required,
        Map<String, Object> properties
) {
}
