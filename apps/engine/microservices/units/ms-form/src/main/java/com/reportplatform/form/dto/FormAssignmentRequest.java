package com.reportplatform.form.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FormAssignmentRequest(
        @NotEmpty List<String> orgIds
) {
}
