package com.reportplatform.ver.repository;

import com.reportplatform.ver.model.VersionDiffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionDiffRepository extends JpaRepository<VersionDiffEntity, UUID> {

    Optional<VersionDiffEntity> findByEntityTypeAndEntityIdAndFromVersionAndToVersion(
            String entityType, UUID entityId, Integer fromVersion, Integer toVersion);
}
