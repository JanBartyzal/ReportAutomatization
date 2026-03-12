package com.reportplatform.ver.model.dto;

import com.reportplatform.ver.model.enums.ChangeType;

public record FieldChange(
        String fieldPath,
        ChangeType changeType,
        Object oldValue,
        Object newValue
) {}
