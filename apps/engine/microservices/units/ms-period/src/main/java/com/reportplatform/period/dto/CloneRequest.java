package com.reportplatform.period.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public record CloneRequest(
        @NotBlank String newName,
        @NotBlank String newPeriodCode,
        @NotNull LocalDate newStartDate,
        @NotNull LocalDate newEndDate,
        @NotNull Instant newSubmissionDeadline,
        @NotNull Instant newReviewDeadline
) {}
