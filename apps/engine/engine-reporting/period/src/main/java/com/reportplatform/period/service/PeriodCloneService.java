package com.reportplatform.period.service;

import com.reportplatform.period.dto.CloneRequest;
import com.reportplatform.period.exception.PeriodNotFoundException;
import com.reportplatform.period.model.PeriodEntity;
import com.reportplatform.period.model.PeriodOrgAssignmentEntity;
import com.reportplatform.period.repository.PeriodOrgAssignmentRepository;
import com.reportplatform.period.repository.PeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PeriodCloneService {

    private static final Logger log = LoggerFactory.getLogger(PeriodCloneService.class);

    private final PeriodRepository periodRepository;
    private final PeriodOrgAssignmentRepository assignmentRepository;

    public PeriodCloneService(PeriodRepository periodRepository,
                              PeriodOrgAssignmentRepository assignmentRepository) {
        this.periodRepository = periodRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public PeriodEntity clonePeriod(UUID sourcePeriodId, CloneRequest request, String userId) {
        var source = periodRepository.findById(sourcePeriodId)
                .orElseThrow(() -> new PeriodNotFoundException(sourcePeriodId));

        var cloned = new PeriodEntity(
                request.newName(),
                source.getPeriodType(),
                request.newPeriodCode(),
                request.newStartDate(),
                request.newEndDate(),
                request.newSubmissionDeadline(),
                request.newReviewDeadline(),
                source.getHoldingId(),
                userId
        );
        cloned.setClonedFromId(sourcePeriodId);
        cloned = periodRepository.save(cloned);

        var sourceAssignments = assignmentRepository.findByPeriodId(sourcePeriodId);
        for (var assignment : sourceAssignments) {
            assignmentRepository.save(new PeriodOrgAssignmentEntity(cloned.getId(), assignment.getOrgId()));
        }

        log.info("Period cloned: source={} -> new={} ({} assignments copied)",
                sourcePeriodId, cloned.getId(), sourceAssignments.size());
        return cloned;
    }
}
