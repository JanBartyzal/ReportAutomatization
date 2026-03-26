package com.reportplatform.period.service;

import com.reportplatform.period.config.PeriodState;
import com.reportplatform.period.config.PeriodType;
import com.reportplatform.period.dto.PeriodCreateRequest;
import com.reportplatform.period.dto.PeriodUpdateRequest;
import com.reportplatform.period.exception.PeriodNotFoundException;
import com.reportplatform.period.model.PeriodEntity;
import com.reportplatform.period.model.PeriodOrgAssignmentEntity;
import com.reportplatform.period.repository.PeriodOrgAssignmentRepository;
import com.reportplatform.period.repository.PeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PeriodService {

    private static final Logger log = LoggerFactory.getLogger(PeriodService.class);

    private final PeriodRepository periodRepository;
    private final PeriodOrgAssignmentRepository assignmentRepository;

    public PeriodService(PeriodRepository periodRepository,
                         PeriodOrgAssignmentRepository assignmentRepository) {
        this.periodRepository = periodRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public PeriodEntity createPeriod(PeriodCreateRequest request, String userId) {
        PeriodType periodType;
        try {
            periodType = request.periodType() != null
                    ? PeriodType.valueOf(request.periodType())
                    : PeriodType.QUARTERLY;
        } catch (IllegalArgumentException e) {
            log.warn("Unknown period type '{}', defaulting to QUARTERLY", request.periodType());
            periodType = PeriodType.QUARTERLY;
        }

        // Provide sensible defaults for null fields
        java.time.LocalDate startDate = request.startDate() != null ? request.startDate() : java.time.LocalDate.now();
        java.time.LocalDate endDate = request.endDate() != null ? request.endDate() : startDate.plusMonths(3);
        java.time.Instant submissionDeadline = request.submissionDeadline() != null
                ? request.submissionDeadline()
                : endDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant reviewDeadline = request.reviewDeadline() != null
                ? request.reviewDeadline()
                : endDate.plusDays(15).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        String holdingId = request.holdingId() != null ? request.holdingId() : "default-holding";
        String basePeriodCode = request.periodCode() != null ? request.periodCode()
                : request.name() != null ? request.name().replaceAll("\\s+", "-") : "PERIOD-" + UUID.randomUUID().toString().substring(0, 8);
        // Ensure uniqueness: if code already exists, append a short suffix
        String periodCode = basePeriodCode;
        if (periodRepository.existsByPeriodCode(periodCode)) {
            periodCode = basePeriodCode + "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        var period = new PeriodEntity(
                request.name(),
                periodType,
                periodCode,
                startDate,
                endDate,
                submissionDeadline,
                reviewDeadline,
                holdingId,
                userId
        );
        period = periodRepository.save(period);

        if (request.orgIds() != null) {
            try {
                for (String orgId : request.orgIds()) {
                    assignmentRepository.save(new PeriodOrgAssignmentEntity(period.getId(), orgId));
                }
            } catch (Exception e) {
                log.warn("Could not save org assignments for period {}: {}", period.getId(), e.getMessage());
            }
        }

        log.info("Period created: id={} code={}", period.getId(), request.periodCode());
        return period;
    }

    public PeriodEntity getPeriod(UUID periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new PeriodNotFoundException(periodId));
    }

    public Page<PeriodEntity> listPeriods(String holdingId, Pageable pageable) {
        if (holdingId != null) {
            return periodRepository.findByHoldingId(holdingId, pageable);
        }
        return periodRepository.findAll(pageable);
    }

    @Transactional
    public PeriodEntity updatePeriod(UUID periodId, PeriodUpdateRequest request) {
        var period = getPeriod(periodId);

        if (request.name() != null) period.setName(request.name());
        if (request.startDate() != null) period.setStartDate(request.startDate());
        if (request.endDate() != null) period.setEndDate(request.endDate());
        if (request.submissionDeadline() != null) period.setSubmissionDeadline(request.submissionDeadline());
        if (request.reviewDeadline() != null) period.setReviewDeadline(request.reviewDeadline());
        if (request.status() != null) period.setStatus(PeriodState.valueOf(request.status()));

        return periodRepository.save(period);
    }

    @Transactional
    public PeriodEntity transitionToCollecting(UUID periodId, String userId) {
        var period = getPeriod(periodId);
        period.setStatus(PeriodState.COLLECTING);
        log.info("Period {} transitioned to COLLECTING by {}", periodId, userId);
        return periodRepository.save(period);
    }

    @Transactional
    public PeriodEntity closePeriod(UUID periodId, String userId) {
        var period = getPeriod(periodId);
        period.setStatus(PeriodState.CLOSED);
        log.info("Period {} closed by {}", periodId, userId);
        return periodRepository.save(period);
    }

    public List<PeriodOrgAssignmentEntity> getAssignments(UUID periodId) {
        return assignmentRepository.findByPeriodId(periodId);
    }
}
