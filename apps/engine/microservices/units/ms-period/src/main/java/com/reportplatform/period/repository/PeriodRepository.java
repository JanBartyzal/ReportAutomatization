package com.reportplatform.period.repository;

import com.reportplatform.period.config.PeriodState;
import com.reportplatform.period.config.PeriodType;
import com.reportplatform.period.model.PeriodEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PeriodRepository extends JpaRepository<PeriodEntity, UUID> {

    Page<PeriodEntity> findByHoldingId(String holdingId, Pageable pageable);

    Page<PeriodEntity> findByStatus(PeriodState status, Pageable pageable);

    List<PeriodEntity> findByStatusAndSubmissionDeadlineBetween(
            PeriodState status, Instant from, Instant to);

    @Query("SELECT p FROM PeriodEntity p WHERE p.status IN ('OPEN', 'COLLECTING') " +
           "AND p.submissionDeadline < :now")
    List<PeriodEntity> findPastDeadline(@Param("now") Instant now);

    List<PeriodEntity> findByPeriodTypeAndHoldingIdOrderByStartDateDesc(
            PeriodType periodType, String holdingId);

    @Query("SELECT p FROM PeriodEntity p WHERE p.holdingId = :holdingId " +
           "AND p.periodType = :type ORDER BY p.startDate DESC")
    List<PeriodEntity> findByTypeAndHolding(
            @Param("type") PeriodType type,
            @Param("holdingId") String holdingId);

    @Query("SELECT p FROM PeriodEntity p WHERE p.id IN :ids ORDER BY p.startDate")
    List<PeriodEntity> findByIdInOrderByStartDate(@Param("ids") List<UUID> ids);
}
