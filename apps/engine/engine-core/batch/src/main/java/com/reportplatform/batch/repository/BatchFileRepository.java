package com.reportplatform.batch.repository;

import com.reportplatform.batch.model.entity.BatchFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchFileRepository extends JpaRepository<BatchFileEntity, UUID> {

    List<BatchFileEntity> findByBatchId(UUID batchId);

    Optional<BatchFileEntity> findByBatchIdAndFileId(UUID batchId, UUID fileId);

    long countByBatchId(UUID batchId);

    void deleteByBatchIdAndFileId(UUID batchId, UUID fileId);
}
