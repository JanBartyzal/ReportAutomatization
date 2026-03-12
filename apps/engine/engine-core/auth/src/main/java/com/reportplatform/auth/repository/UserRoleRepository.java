package com.reportplatform.auth.repository;

import com.reportplatform.auth.model.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    List<UserRoleEntity> findByUserOid(String userOid);

    List<UserRoleEntity> findByUserOidAndOrganizationId(String userOid, UUID organizationId);

    Optional<UserRoleEntity> findByUserOidAndActiveOrgTrue(String userOid);

    @Modifying
    @Query("UPDATE UserRoleEntity ur SET ur.activeOrg = false WHERE ur.userOid = :userOid")
    void clearActiveOrg(@Param("userOid") String userOid);

    @Modifying
    @Query("UPDATE UserRoleEntity ur SET ur.activeOrg = true WHERE ur.userOid = :userOid AND ur.organization.id = :orgId")
    void setActiveOrg(@Param("userOid") String userOid, @Param("orgId") UUID orgId);
}
