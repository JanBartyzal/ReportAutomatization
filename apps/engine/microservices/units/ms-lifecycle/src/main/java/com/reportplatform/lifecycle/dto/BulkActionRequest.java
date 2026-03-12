package com.reportplatform.lifecycle.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BulkActionRequest(
        @NotEmpty List<UUID> reportIds,
        String comment
) {}
