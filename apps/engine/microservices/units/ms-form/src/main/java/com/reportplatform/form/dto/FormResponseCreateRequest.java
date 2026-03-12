package com.reportplatform.form.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public record FormResponseCreateRequest(
        @NotBlank String orgId,
        UUID periodId,
        Map<String, Object> data
) {
}
