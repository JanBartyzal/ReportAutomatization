package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.StorageRoutingConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for storage routing configuration rules.
 */
public interface StorageRoutingConfigRepository extends JpaRepository<StorageRoutingConfigEntity, UUID> {

    /**
     * Load all rules effective at the given timestamp, ordered from most
     * specific (org+source) to least specific (global default).
     * The service picks the first matching rule.
     */
    @Query("""
            SELECT r FROM StorageRoutingConfigEntity r
            WHERE r.effectiveFrom <= :now
            ORDER BY
                CASE WHEN r.orgId IS NOT NULL AND r.sourceType IS NOT NULL THEN 0
                     WHEN r.orgId IS NOT NULL                              THEN 1
                     WHEN r.sourceType IS NOT NULL                         THEN 2
                     ELSE 3
                END ASC,
                r.effectiveFrom DESC
            """)
    List<StorageRoutingConfigEntity> findAllEffective(@Param("now") OffsetDateTime now);

    /** All rules that target a specific org (for the admin UI listing). */
    List<StorageRoutingConfigEntity> findByOrgId(UUID orgId);

    /** Retrieve the global default rule (both discriminators NULL). */
    @Query("SELECT r FROM StorageRoutingConfigEntity r WHERE r.orgId IS NULL AND r.sourceType IS NULL")
    java.util.Optional<StorageRoutingConfigEntity> findGlobalDefault();
}
