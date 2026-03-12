package com.reportplatform.lifecycle.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank String comment
) {}
