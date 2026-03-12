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
        var period = new PeriodEntity(
                request.name(),
                PeriodType.valueOf(request.periodType()),
                request.periodCode(),
                request.startDate(),
                request.endDate(),
                request.submissionDeadline(),
                request.reviewDeadline(),
                request.holdingId(),
                userId
        );
        period = periodRepository.save(period);

        if (request.orgIds() != null) {
            for (String orgId : request.orgIds()) {
                assignmentRepository.save(new PeriodOrgAssignmentEntity(period.getId(), orgId));
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

    public List<PeriodOrgAssignmentEntity> getAssignments(UUID periodId) {
        return assignmentRepository.findByPeriodId(periodId);
    }
}
