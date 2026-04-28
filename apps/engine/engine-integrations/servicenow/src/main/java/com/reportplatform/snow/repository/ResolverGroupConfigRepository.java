package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.ResolverGroupConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResolverGroupConfigRepository extends JpaRepository<ResolverGroupConfigEntity, UUID> {

    List<ResolverGroupConfigEntity> findByConnectionId(UUID connectionId);

    List<ResolverGroupConfigEntity> findByConnectionIdAndSyncEnabledTrue(UUID connectionId);

    Optional<ResolverGroupConfigEntity> findByConnectionIdAndGroupSysId(UUID connectionId, String groupSysId);

    boolean existsByConnectionIdAndGroupSysId(UUID connectionId, String groupSysId);
}
