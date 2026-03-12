package com.reportplatform.period.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CrossTypeComparisonRequest(
        @NotEmpty List<UUID> periodIds,
        @NotNull String holdingId
) {}
