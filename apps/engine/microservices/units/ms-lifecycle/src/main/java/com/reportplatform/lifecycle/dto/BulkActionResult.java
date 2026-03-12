package com.reportplatform.lifecycle.dto;

import java.util.List;
import java.util.UUID;

public record BulkActionResult(
        int total,
        int succeeded,
        int failed,
        List<UUID> failedIds
) {}
