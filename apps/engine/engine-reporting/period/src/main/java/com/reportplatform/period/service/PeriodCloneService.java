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

        // Derive defaults from source period when clone request has null fields
        String cloneName = request.newName() != null ? request.newName() : source.getName() + " (clone)";
        // Always append a random suffix to ensure period_code uniqueness (column has UNIQUE constraint)
        String baseCode = request.newPeriodCode() != null ? request.newPeriodCode() : source.getPeriodCode() + "-CLONE";
        String cloneCode = baseCode + "-" + UUID.randomUUID().toString().substring(0, 4);
        java.time.LocalDate cloneStart = request.newStartDate() != null ? request.newStartDate() : source.getStartDate();
        java.time.LocalDate cloneEnd = request.newEndDate() != null ? request.newEndDate()
                : (request.newStartDate() != null ? request.newStartDate().plusMonths(3) : source.getEndDate());
        java.time.Instant cloneSubDeadline = request.newSubmissionDeadline() != null ? request.newSubmissionDeadline()
                : source.getSubmissionDeadline();
        java.time.Instant cloneRevDeadline = request.newReviewDeadline() != null ? request.newReviewDeadline()
                : source.getReviewDeadline();

        var cloned = new PeriodEntity(
                cloneName,
                source.getPeriodType(),
                cloneCode,
                cloneStart,
                cloneEnd,
                cloneSubDeadline,
                cloneRevDeadline,
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
