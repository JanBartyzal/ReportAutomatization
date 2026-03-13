package com.reportplatform.period.service;

import com.reportplatform.period.config.PeriodState;
import com.reportplatform.period.model.PeriodEntity;
import com.reportplatform.period.repository.PeriodOrgAssignmentRepository;
import com.reportplatform.period.repository.PeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class DeadlineService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineService.class);

    private final PeriodRepository periodRepository;
    private final PeriodOrgAssignmentRepository assignmentRepository;
    private final DaprEventPublisher eventPublisher;

    @Value("${period.deadline.reminder-days:7,3,1}")
    private String reminderDays;

    public DeadlineService(PeriodRepository periodRepository,
            PeriodOrgAssignmentRepository assignmentRepository,
            DaprEventPublisher eventPublisher) {
        this.periodRepository = periodRepository;
        this.assignmentRepository = assignmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${period.deadline.check-cron:0 0 8 * * *}")
    @Transactional
    public void checkDeadlines() {
        log.info("Running deadline check...");

        List<Integer> days = Arrays.stream(reminderDays.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();

        for (int daysBefore : days) {
            checkRemindersForDays(daysBefore);
        }

        checkPastDeadlines();

        log.info("Deadline check completed");
    }

    private void checkRemindersForDays(int daysBefore) {
        Instant from = Instant.now().plus(daysBefore, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        Instant to = from.plus(1, ChronoUnit.DAYS);

        List<PeriodEntity> periods = periodRepository
                .findByStatusAndSubmissionDeadlineBetween(PeriodState.COLLECTING, from, to);

        for (PeriodEntity period : periods) {
            List<String> orgIds = assignmentRepository.findByPeriodId(period.getId()).stream()
                    .map(a -> a.getOrgId())
                    .toList();

            if (!orgIds.isEmpty()) {
                eventPublisher.publishDeadlineReminder(
                        period.getId(), period.getName(), daysBefore, orgIds);
            }
        }
    }

    @Transactional
    public void checkPastDeadlines() {
        List<PeriodEntity> pastDeadline = periodRepository.findPastDeadline(Instant.now());

        for (PeriodEntity period : pastDeadline) {
            period.setStatus(PeriodState.REVIEWING);
            periodRepository.save(period);

            List<String> orgIds = assignmentRepository.findByPeriodId(period.getId()).stream()
                    .map(a -> a.getOrgId())
                    .toList();

            eventPublisher.publishDeadlineEscalation(period.getId(), period.getName(), orgIds);

            log.info("Period {} deadline passed, moved to REVIEWING", period.getId());
        }

        // Auto-close periods past their review deadline
        checkPastReviewDeadlines();
    }

    @Transactional
    public void checkPastReviewDeadlines() {
        List<PeriodEntity> pastReviewDeadline = periodRepository.findPastReviewDeadline(Instant.now());

        for (PeriodEntity period : pastReviewDeadline) {
            period.setStatus(PeriodState.CLOSED);
            periodRepository.save(period);

            List<String> orgIds = assignmentRepository.findByPeriodId(period.getId()).stream()
                    .map(a -> a.getOrgId())
                    .toList();

            eventPublisher.publishPeriodAutoClosed(period.getId(), period.getName(), orgIds);

            log.info("Period {} review deadline passed, auto-closed", period.getId());
        }
    }
}
