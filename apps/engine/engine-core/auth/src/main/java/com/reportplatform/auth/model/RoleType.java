package com.reportplatform.auth.model;

/**
 * Role hierarchy: HOLDING_ADMIN (0) > ADMIN (1) > EDITOR (2) > VIEWER (3).
 * Lower level means higher privilege.
 */
public enum RoleType {

    HOLDING_ADMIN(0, "holding-admin"),
    ADMIN(1, "admin"),
    COMPANY_ADMIN(2, "company-admin"),
    EDITOR(3, "editor"),
    VIEWER(4, "viewer");

    private final int hierarchyLevel;
    private final String aadGroupSuffix;

    RoleType(int hierarchyLevel, String aadGroupSuffix) {
        this.hierarchyLevel = hierarchyLevel;
        this.aadGroupSuffix = aadGroupSuffix;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    public String getAadGroupSuffix() {
        return aadGroupSuffix;
    }

    /**
     * Returns true if this role has equal or higher privilege than the required role.
     */
    public boolean isAtLeast(RoleType required) {
        return this.hierarchyLevel <= required.hierarchyLevel;
    }

    /**
     * Maps an AAD security group name to an internal role.
     * Expected group format: "ReportPlatform-{orgCode}-{roleSuffix}"
     */
    public static RoleType fromAadGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return VIEWER;
        }
        String lower = groupName.toLowerCase();
        for (RoleType role : values()) {
            if (lower.endsWith("-" + role.aadGroupSuffix)) {
                return role;
            }
        }
        return VIEWER;
    }
}
