package com.reportplatform.enginedata.common.config;

/**
 * ThreadLocal holder for RLS context (org_id, user_id, role).
 * Set by HeaderAuthenticationFilter, read by RlsDataSourceWrapper.
 */
public final class RlsContext {

    private static final ThreadLocal<String> ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    private RlsContext() {}

    public static void setOrgId(String orgId) { ORG_ID.set(orgId); }
    public static String getOrgId() { return ORG_ID.get(); }

    public static void setUserId(String userId) { USER_ID.set(userId); }
    public static String getUserId() { return USER_ID.get(); }

    public static void setRole(String role) { ROLE.set(role); }
    public static String getRole() { return ROLE.get(); }

    public static void clear() {
        ORG_ID.remove();
        USER_ID.remove();
        ROLE.remove();
    }
}
