package com.reportplatform.period.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PeriodUpdateRequest(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Instant submissionDeadline,
        Instant reviewDeadline,
        String status
) {}
