package com.reportplatform.ver.repository;

import com.reportplatform.ver.model.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionRepository extends JpaRepository<VersionEntity, UUID> {

    List<VersionEntity> findByEntityTypeAndEntityIdOrderByVersionNumberDesc(
            String entityType, UUID entityId);

    Optional<VersionEntity> findByEntityTypeAndEntityIdAndVersionNumber(
            String entityType, UUID entityId, Integer versionNumber);

    Optional<VersionEntity> findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(
            String entityType, UUID entityId);

    @Modifying
    @Query("UPDATE VersionEntity v SET v.locked = true WHERE v.entityType = :entityType AND v.entityId = :entityId")
    int lockAllVersions(@Param("entityType") String entityType, @Param("entityId") UUID entityId);
}
