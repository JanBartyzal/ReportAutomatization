package com.reportplatform.auth.model.dto;

import java.util.List;

public record UserContextResponse(
    String userId,
    String displayName,
    String email,
    List<OrgRole> organizations,
    String activeOrganizationId
) {

    public record OrgRole(
        String organizationId,
        String organizationCode,
        String organizationName,
        List<String> roles
    ) {
    }
}
