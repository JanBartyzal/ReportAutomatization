package com.reportplatform.ver.model.dto;

import java.util.List;
import java.util.UUID;

public record VersionDiffResponse(
        String entityType,
        UUID entityId,
        Integer fromVersion,
        Integer toVersion,
        List<FieldChange> changes
) {}
