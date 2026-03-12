package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.RoleAssignmentRequest;
import com.reportplatform.admin.model.dto.RoleAssignmentResponse;
import com.reportplatform.admin.model.entity.RoleAuditLogEntity;
import com.reportplatform.admin.repository.RoleAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleManagementService {

    private static final Logger log = LoggerFactory.getLogger(RoleManagementService.class);

    private static final Set<String> VALID_ROLES = Set.of(
            "HOLDING_ADMIN", "ADMIN", "COMPANY_ADMIN", "EDITOR", "VIEWER"
    );

    private static final Set<String> ROLES_REQUIRING_HOLDING_ADMIN = Set.of(
            "HOLDING_ADMIN", "ADMIN"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RoleAuditLogRepository auditLogRepository;

    public RoleManagementService(NamedParameterJdbcTemplate jdbcTemplate,
                                  RoleAuditLogRepository auditLogRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public RoleAssignmentResponse assignRole(RoleAssignmentRequest request,
                                              String performedBy, String callerRole,
                                              String ipAddress) {
        validateRole(request.role());
        validateCallerPermission(request.role(), callerRole);

        var params = new MapSqlParameterSource();
        params.addValue("userOid", request.targetUserId());
        params.addValue("orgId", request.orgId());
        params.addValue("role", request.role());

        jdbcTemplate.update(
                "INSERT INTO user_roles (id, user_oid, organization_id, role) " +
                "VALUES (gen_random_uuid(), :userOid, :orgId, :role) " +
                "ON CONFLICT (user_oid, organization_id, role) DO NOTHING",
                params
        );

        logAudit(performedBy, request.targetUserId(), RoleAuditLogEntity.AuditAction.ASSIGN,
                request.role(), request.orgId(), ipAddress);

        log.info("Role {} assigned to user {} in org {} by {}",
                request.role(), request.targetUserId(), request.orgId(), performedBy);

        return new RoleAssignmentResponse(
                request.targetUserId(), request.orgId(), request.role(),
                performedBy, Instant.now()
        );
    }

    @Transactional
    public void revokeRole(RoleAssignmentRequest request, String performedBy,
                           String callerRole, String ipAddress) {
        validateRole(request.role());
        validateCallerPermission(request.role(), callerRole);

        var params = new MapSqlParameterSource();
        params.addValue("userOid", request.targetUserId());
        params.addValue("orgId", request.orgId());
        params.addValue("role", request.role());

        int deleted = jdbcTemplate.update(
                "DELETE FROM user_roles WHERE user_oid = :userOid " +
                "AND organization_id = :orgId AND role = :role",
                params
        );

        if (deleted > 0) {
            logAudit(performedBy, request.targetUserId(), RoleAuditLogEntity.AuditAction.REMOVE,
                    request.role(), request.orgId(), ipAddress);
            log.info("Role {} revoked from user {} in org {} by {}",
                    request.role(), request.targetUserId(), request.orgId(), performedBy);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRolesForOrg(UUID orgId) {
        var params = new MapSqlParameterSource("orgId", orgId);
        return jdbcTemplate.queryForList(
                "SELECT user_oid, role, created_at FROM user_roles " +
                "WHERE organization_id = :orgId ORDER BY created_at",
                params
        );
    }

    @Transactional(readOnly = true)
    public Page<RoleAuditLogEntity> getAuditLog(int page, int pageSize) {
        return auditLogRepository.findAll(PageRequest.of(page - 1, pageSize));
    }

    private void validateRole(String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid: " + VALID_ROLES);
        }
    }

    private void validateCallerPermission(String targetRole, String callerRole) {
        if (ROLES_REQUIRING_HOLDING_ADMIN.contains(targetRole)
                && !"HOLDING_ADMIN".equals(callerRole)) {
            throw new IllegalStateException(
                    "Only HOLDING_ADMIN can assign/revoke " + targetRole);
        }
        if ("COMPANY_ADMIN".equals(targetRole)
                && !Set.of("HOLDING_ADMIN", "ADMIN").contains(callerRole)) {
            throw new IllegalStateException(
                    "Only HOLDING_ADMIN or ADMIN can assign/revoke COMPANY_ADMIN");
        }
    }

    private void logAudit(String performedBy, String targetUserId,
                          RoleAuditLogEntity.AuditAction action,
                          String role, UUID orgId, String ipAddress) {
        var auditLog = new RoleAuditLogEntity();
        auditLog.setUserId(performedBy);
        auditLog.setTargetUserId(targetUserId);
        auditLog.setAction(action);
        auditLog.setRole(role);
        auditLog.setOrgId(orgId);
        auditLog.setPerformedBy(performedBy);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }
}
