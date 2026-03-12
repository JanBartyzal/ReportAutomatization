package com.reportplatform.batch.repository;

import com.reportplatform.batch.model.entity.BatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<BatchEntity, UUID> {

    @Query("SELECT b FROM BatchEntity b WHERE b.holdingId = :holdingId ORDER BY b.createdAt DESC")
    List<BatchEntity> findByHoldingId(UUID holdingId);

    @Query("SELECT b FROM BatchEntity b WHERE b.holdingId = :holdingId AND b.status = :status")
    List<BatchEntity> findByHoldingIdAndStatus(UUID holdingId, BatchEntity.BatchStatus status);

    @Query("SELECT b FROM BatchEntity b WHERE b.period = :period AND b.holdingId = :holdingId")
    Optional<BatchEntity> findByPeriodAndHoldingId(String period, UUID holdingId);
}
