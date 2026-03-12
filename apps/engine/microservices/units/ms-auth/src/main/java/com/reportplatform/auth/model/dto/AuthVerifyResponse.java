package com.reportplatform.auth.model.dto;

import java.util.List;

public record AuthVerifyResponse(
    String userId,
    String tenantId,
    String organizationId,
    List<String> roles,
    boolean valid
) {
}
