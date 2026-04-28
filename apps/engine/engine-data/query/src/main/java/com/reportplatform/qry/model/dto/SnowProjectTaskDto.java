package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SnowProjectTaskDto(
        UUID id,
        String sysId,
        String number,
        String shortDescription,
        String parentSysId,
        String state,
        boolean milestone,
        String assignedToName,
        LocalDate dueDate,
        Instant completedAt
) {}
