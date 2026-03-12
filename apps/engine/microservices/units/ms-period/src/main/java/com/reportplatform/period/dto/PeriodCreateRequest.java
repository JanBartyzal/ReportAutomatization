package com.reportplatform.period.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PeriodCreateRequest(
        @NotBlank String name,
        @NotBlank String periodType,
        @NotBlank String periodCode,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull Instant submissionDeadline,
        @NotNull Instant reviewDeadline,
        @NotBlank String holdingId,
        List<String> orgIds
) {}
