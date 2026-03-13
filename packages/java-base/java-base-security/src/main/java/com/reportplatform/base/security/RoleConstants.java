package com.reportplatform.base.security;

/**
 * Shared role constants and SpEL expressions for @PreAuthorize annotations.
 * Role hierarchy: HOLDING_ADMIN > ADMIN > EDITOR > VIEWER, COMPANY_ADMIN > EDITOR
 */
public final class RoleConstants {

    private RoleConstants() {}

    public static final String VIEWER = "VIEWER";
    public static final String EDITOR = "EDITOR";
    public static final String ADMIN = "ADMIN";
    public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
    public static final String HOLDING_ADMIN = "HOLDING_ADMIN";

    // SpEL expressions for @PreAuthorize (with role hierarchy, hasRole('VIEWER') includes all higher roles)
    public static final String HAS_VIEWER = "hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')";
    public static final String HAS_EDITOR = "hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')";
    public static final String HAS_ADMIN = "hasAnyRole('ADMIN','HOLDING_ADMIN')";
    public static final String HAS_HOLDING_ADMIN = "hasRole('HOLDING_ADMIN')";
    public static final String IS_AUTHENTICATED = "isAuthenticated()";
}
