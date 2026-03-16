package com.reportplatform.notif.service;

import com.reportplatform.notif.dto.NotificationEvent;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service for deadline-related operations.
 * Communicates with MS-PERIOD and MS-LIFECYCLE via Dapr.
 */
@Service("notificationDeadlineService")
@Slf4j
public class DeadlineService {

    private final DaprClient daprClient;
    private final NotificationService notificationService;

    @Value("${app.dapr.period-service:ms-period}")
    private String periodServiceId;

    @Value("${app.dapr.lifecycle-service:ms-lifecycle}")
    private String lifecycleServiceId;

    public DeadlineService(NotificationService notificationService) {
        this.daprClient = new DaprClientBuilder().build();
        this.notificationService = notificationService;
    }

    /**
     * Get periods with deadline approaching in the specified number of days.
     */
    public List<String> getPeriodsWithDeadlineApproaching(int days) {
        try {
            // Call MS-PERIOD service to get periods with upcoming deadlines
            // This would normally use Dapr service invocation
            String result = daprClient.invokeMethod(
                    periodServiceId,
                    "/api/v1/periods/deadlines/approaching?days=" + days,
                    null,
                    io.dapr.client.domain.HttpExtension.GET,
                    String.class).block();

            if (result != null && !result.isBlank()) {
                return List.of(result.split(","));
            }
        } catch (Exception e) {
            log.warn("Error getting periods with deadline approaching: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Get periods with missed deadlines.
     */
    public List<String> getPeriodsWithMissedDeadlines() {
        try {
            String result = daprClient.invokeMethod(
                    periodServiceId,
                    "/api/v1/periods/deadlines/missed",
                    null,
                    io.dapr.client.domain.HttpExtension.GET,
                    String.class).block();

            if (result != null && !result.isBlank()) {
                return List.of(result.split(","));
            }
        } catch (Exception e) {
            log.warn("Error getting periods with missed deadlines: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Get user IDs with DRAFT reports for a period.
     */
    public List<String> getDraftUserIdsForPeriod(String periodId) {
        try {
            String result = daprClient.invokeMethod(
                    lifecycleServiceId,
                    "/api/v1/reports/draft-users?periodId=" + periodId,
                    null,
                    io.dapr.client.domain.HttpExtension.GET,
                    String.class).block();

            if (result != null && !result.isBlank()) {
                return List.of(result.split(","));
            }
        } catch (Exception e) {
            log.warn("Error getting draft user IDs for period {}: {}", periodId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Get HoldingAdmin ID for a period.
     */
    public String getHoldingAdminIdForPeriod(String periodId) {
        try {
            return daprClient.invokeMethod(
                    periodServiceId,
                    "/api/v1/periods/" + periodId + "/holding-admin",
                    null,
                    io.dapr.client.domain.HttpExtension.GET,
                    String.class).block();
        } catch (Exception e) {
            log.warn("Error getting HoldingAdmin ID for period {}: {}", periodId, e.getMessage());
        }
        return null;
    }

    /**
     * Get non-compliant organizations for a period.
     */
    public List<String> getNonCompliantOrgsForPeriod(String periodId) {
        try {
            String result = daprClient.invokeMethod(
                    lifecycleServiceId,
                    "/api/v1/reports/non-compliant?periodId=" + periodId,
                    null,
                    io.dapr.client.domain.HttpExtension.GET,
                    String.class).block();

            if (result != null && !result.isBlank()) {
                return List.of(result.split(","));
            }
        } catch (Exception e) {
            log.warn("Error getting non-compliant orgs for period {}: {}", periodId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Send notification via the notification service.
     */
    public void sendNotification(NotificationEvent event) {
        notificationService.createNotification(event);
    }
}
