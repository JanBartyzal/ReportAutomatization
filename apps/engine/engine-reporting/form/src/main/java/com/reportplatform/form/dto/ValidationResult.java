package com.reportplatform.form.dto;

import java.util.List;

public record ValidationResult(
        boolean valid,
        List<FieldError> errors,
        List<FieldError> warnings
) {
    public record FieldError(
            String fieldKey,
            String message,
            String rule
    ) {
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of(), List.of());
    }

    public static ValidationResult withErrors(List<FieldError> errors) {
        return new ValidationResult(false, errors, List.of());
    }

    public static ValidationResult withWarnings(List<FieldError> warnings) {
        return new ValidationResult(true, List.of(), warnings);
    }

    public static ValidationResult of(List<FieldError> errors, List<FieldError> warnings) {
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
}
