package com.reportplatform.snow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for registering a new resolver group to monitor. */
public record CreateResolverGroupRequest(

        @NotBlank @Size(max = 32)
        String groupSysId,

        @NotBlank @Size(max = 255)
        String groupName,

        /** JSON array of data types: ["INCIDENT"], ["REQUEST"], ["INCIDENT","REQUEST","TASK"]. */
        String dataTypes,

        boolean syncEnabled
) {
    public CreateResolverGroupRequest {
        if (dataTypes == null || dataTypes.isBlank()) dataTypes = "[\"INCIDENT\"]";
    }
}
