package com.reportplatform.admin.model.dto;

public record DualWriteInfoResponse(
        String dualWriteUntil,
        boolean active) {
}
