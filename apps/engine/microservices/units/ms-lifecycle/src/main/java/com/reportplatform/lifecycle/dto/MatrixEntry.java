package com.reportplatform.lifecycle.dto;

import java.util.UUID;

public record MatrixEntry(
        String orgId,
        UUID periodId,
        String status,
        String scope,
        long count
) {}
