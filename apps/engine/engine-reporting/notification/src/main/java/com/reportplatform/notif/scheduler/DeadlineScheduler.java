package com.reportplatform.notif.scheduler;

import com.reportplatform.notif.dto.NotificationEvent;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.service.DeadlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduled job for deadline notifications.
 * Runs daily at 08:00 UTC to check for upcoming deadlines.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineScheduler {

    private final DeadlineService deadlineService;

    /**
     * Run daily at 08:00 UTC to check for upcoming deadlines.
     */
    @Scheduled(cron = "0 0 8 * * ?", zone = "UTC")
    public void checkUpcomingDeadlines() {
        log.info("Running scheduled deadline check at {}", Instant.now());

        try {
            // Check for deadlines approaching (7, 3, 1 day)
            processUpcomingDeadlines();

            // Check for missed deadlines
            processMissedDeadlines();

        } catch (Exception e) {
            log.error("Error in scheduled deadline check: {}", e.getMessage(), e);
        }
    }

    /**
     * Process notifications for approaching deadlines.
     */
    private void processUpcomingDeadlines() {
        // Check 7 days before
        deadlineService.getPeriodsWithDeadlineApproaching(7).forEach(period -> {
            sendDeadlineNotification(period, 7);
        });

        // Check 3 days before
        deadlineService.getPeriodsWithDeadlineApproaching(3).forEach(period -> {
            sendDeadlineNotification(period, 3);
        });

        // Check 1 day before
        deadlineService.getPeriodsWithDeadlineApproaching(1).forEach(period -> {
            sendDeadlineNotification(period, 1);
        });
    }

    /**
     * Process notifications for missed deadlines.
     */
    private void processMissedDeadlines() {
        deadlineService.getPeriodsWithMissedDeadlines().forEach(period -> {
            sendMissedDeadlineNotification(period);
        });
    }

    /**
     * Send deadline approaching notification.
     */
    private void sendDeadlineNotification(String periodId, int daysUntil) {
        List<String> draftUserIds = deadlineService.getDraftUserIdsForPeriod(periodId);

        if (!draftUserIds.isEmpty()) {
            String userIds = String.join(",", draftUserIds);

            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.DEADLINE_APPROACHING)
                    .title("Deadline Approaching")
                    .body(String.format(
                            "Your report for period %s is due in %d day(s). Please complete your submission.", periodId,
                            daysUntil))
                    .data(Map.of(
                            "periodId", periodId,
                            "daysUntil", String.valueOf(daysUntil),
                            "userIds", userIds))
                    .build();

            deadlineService.sendNotification(event);
            log.info("Sent deadline approaching notification for period {} to {} users", periodId, draftUserIds.size());
        }
    }

    /**
     * Send missed deadline notification to HoldingAdmin.
     */
    private void sendMissedDeadlineNotification(String periodId) {
        String holdingAdminId = deadlineService.getHoldingAdminIdForPeriod(periodId);
        List<String> nonCompliantOrgs = deadlineService.getNonCompliantOrgsForPeriod(periodId);

        if (holdingAdminId != null && !nonCompliantOrgs.isEmpty()) {
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.DEADLINE_MISSED)
                    .title("Deadline Missed")
                    .body(String.format(
                            "The submission deadline for period %s has passed. %d organization(s) have not submitted their reports: %s",
                            periodId, nonCompliantOrgs.size(), String.join(", ", nonCompliantOrgs)))
                    .data(Map.of(
                            "periodId", periodId,
                            "holdingAdminId", holdingAdminId,
                            "nonCompliantOrgs", String.join(",", nonCompliantOrgs)))
                    .targetUserId(holdingAdminId)
                    .build();

            deadlineService.sendNotification(event);
            log.info("Sent deadline missed notification for period {} to HoldingAdmin {}", periodId, holdingAdminId);
        }
    }
}
