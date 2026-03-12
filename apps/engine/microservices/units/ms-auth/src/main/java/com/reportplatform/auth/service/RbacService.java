package com.reportplatform.auth.service;

import com.reportplatform.auth.model.RoleType;
import com.reportplatform.auth.model.UserRoleEntity;
import com.reportplatform.auth.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);

    /**
     * Mapping from AAD security group name patterns to internal roles.
     * Group naming convention: "ReportPlatform-{orgCode}-{roleSuffix}"
     */
    private static final Map<String, RoleType> AAD_GROUP_ROLE_MAP = Map.of(
            "holding-admin", RoleType.HOLDING_ADMIN,
            "admin", RoleType.ADMIN,
            "company-admin", RoleType.COMPANY_ADMIN,
            "editor", RoleType.EDITOR,
            "viewer", RoleType.VIEWER
    );

    private final UserRoleRepository userRoleRepository;

    public RbacService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Returns all roles the user has for a specific organization.
     */
    @Transactional(readOnly = true)
    public List<RoleType> getUserRolesForOrg(String userOid, UUID organizationId) {
        return userRoleRepository.findByUserOidAndOrganizationId(userOid, organizationId)
                .stream()
                .map(UserRoleEntity::getRole)
                .toList();
    }

    /**
     * Returns all user role assignments across all organizations.
     */
    @Transactional(readOnly = true)
    public List<UserRoleEntity> getAllUserRoles(String userOid) {
        return userRoleRepository.findByUserOid(userOid);
    }

    /**
     * Checks if the user has the required role (or higher) for the given organization.
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(String userOid, UUID organizationId, RoleType requiredRole) {
        List<RoleType> roles = getUserRolesForOrg(userOid, organizationId);
        if (roles.isEmpty()) {
            log.debug("No roles found for user {} in org {}", userOid, organizationId);
            return false;
        }

        boolean permitted = roles.stream().anyMatch(role -> role.isAtLeast(requiredRole));
        log.debug("Permission check: user={}, org={}, required={}, granted={}",
                userOid, organizationId, requiredRole, permitted);
        return permitted;
    }

    /**
     * Gets the highest-privilege role the user has in the given organization.
     */
    @Transactional(readOnly = true)
    public RoleType getHighestRole(String userOid, UUID organizationId) {
        return getUserRolesForOrg(userOid, organizationId)
                .stream()
                .min((a, b) -> Integer.compare(a.getHierarchyLevel(), b.getHierarchyLevel()))
                .orElse(null);
    }

    /**
     * Maps AAD security group names to internal roles.
     * Groups matching the pattern "ReportPlatform-{orgCode}-{roleSuffix}" are mapped.
     *
     * @param groupNames list of AAD security group names
     * @return map of orgCode to list of internal roles
     */
    public Map<String, List<RoleType>> mapAadGroupsToRoles(List<String> groupNames) {
        if (groupNames == null || groupNames.isEmpty()) {
            return Collections.emptyMap();
        }

        return groupNames.stream()
                .filter(g -> g.startsWith("ReportPlatform-"))
                .collect(Collectors.groupingBy(
                        g -> extractOrgCode(g),
                        Collectors.mapping(
                                g -> RoleType.fromAadGroup(g),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * Extracts org code from group name.
     * Format: "ReportPlatform-{orgCode}-{roleSuffix}"
     */
    private String extractOrgCode(String groupName) {
        String[] parts = groupName.split("-");
        if (parts.length >= 3) {
            // Everything between first and last dash segment
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < parts.length - 1; i++) {
                if (i > 1) sb.append("-");
                sb.append(parts[i]);
            }
            return sb.toString();
        }
        return groupName;
    }
}
