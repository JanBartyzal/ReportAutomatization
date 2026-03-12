package com.reportplatform.enginereporting.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * InternalEventBridge replaces Dapr PubSub network calls with in-process Spring ApplicationEvent
 * for intra-service communication within the consolidated engine-reporting service.
 *
 * Modules publish events via this bridge instead of Dapr when the target module is co-located.
 * External services (outside engine-reporting) still receive events via Dapr PubSub.
 */
@Component
public class InternalEventBridge {

    private static final Logger log = LoggerFactory.getLogger(InternalEventBridge.class);

    private final ApplicationEventPublisher eventPublisher;

    public InternalEventBridge(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publish a report status changed event (lifecycle -> notification, period, form).
     */
    public void publishReportStatusChanged(UUID reportId, String orgId,
                                           String fromStatus, String toStatus, String userId) {
        var event = new ReportStatusChangedInternalEvent(this, reportId, orgId, fromStatus, toStatus, userId);
        eventPublisher.publishEvent(event);
        log.info("Internal event: report.status_changed reportId={}, {} -> {}", reportId, fromStatus, toStatus);
    }

    /**
     * Publish a deadline event (period -> notification).
     */
    public void publishDeadlineReminder(UUID periodId, String periodName,
                                        int daysRemaining, List<String> orgIds) {
        var event = new DeadlineInternalEvent(this, periodId, periodName,
                DeadlineInternalEvent.DeadlineType.REMINDER, daysRemaining, orgIds);
        eventPublisher.publishEvent(event);
        log.info("Internal event: deadline.reminder periodId={}, days={}", periodId, daysRemaining);
    }

    /**
     * Publish a deadline escalation event (period -> notification).
     */
    public void publishDeadlineEscalation(UUID periodId, String periodName, List<String> orgIds) {
        var event = new DeadlineInternalEvent(this, periodId, periodName,
                DeadlineInternalEvent.DeadlineType.ESCALATION, 0, orgIds);
        eventPublisher.publishEvent(event);
        log.info("Internal event: deadline.escalation periodId={}", periodId);
    }

    /**
     * Publish a local report released event (lifecycle -> notification).
     */
    public void publishLocalReleased(UUID reportId, String orgId, String userId) {
        var event = new LocalReleasedInternalEvent(this, reportId, orgId, userId);
        eventPublisher.publishEvent(event);
        log.info("Internal event: report.local_released reportId={}", reportId);
    }

    /**
     * Publish a data locked event (lifecycle -> notification).
     */
    public void publishDataLocked(UUID reportId, String orgId, String userId) {
        var event = new DataLockedInternalEvent(this, reportId, orgId, userId);
        eventPublisher.publishEvent(event);
        log.info("Internal event: report.data_locked reportId={}", reportId);
    }

    // ============================================================
    // Event classes
    // ============================================================

    /**
     * Event fired when a report status changes.
     */
    public static class ReportStatusChangedInternalEvent extends org.springframework.context.ApplicationEvent {
        private final UUID reportId;
        private final String orgId;
        private final String fromStatus;
        private final String toStatus;
        private final String userId;
        private final long timestamp;

        public ReportStatusChangedInternalEvent(Object source, UUID reportId, String orgId,
                                                String fromStatus, String toStatus, String userId) {
            super(source);
            this.reportId = reportId;
            this.orgId = orgId;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getReportId() { return reportId; }
        public String getOrgId() { return orgId; }
        public String getFromStatus() { return fromStatus; }
        public String getToStatus() { return toStatus; }
        public String getUserId() { return userId; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Event fired for deadline reminders and escalations.
     */
    public static class DeadlineInternalEvent extends org.springframework.context.ApplicationEvent {
        public enum DeadlineType { REMINDER, ESCALATION }

        private final UUID periodId;
        private final String periodName;
        private final DeadlineType deadlineType;
        private final int daysRemaining;
        private final List<String> orgIds;
        private final long timestamp;

        public DeadlineInternalEvent(Object source, UUID periodId, String periodName,
                                     DeadlineType deadlineType, int daysRemaining, List<String> orgIds) {
            super(source);
            this.periodId = periodId;
            this.periodName = periodName;
            this.deadlineType = deadlineType;
            this.daysRemaining = daysRemaining;
            this.orgIds = orgIds;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public DeadlineType getDeadlineType() { return deadlineType; }
        public int getDaysRemaining() { return daysRemaining; }
        public List<String> getOrgIds() { return orgIds; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Event fired when a local report is released to central flow.
     */
    public static class LocalReleasedInternalEvent extends org.springframework.context.ApplicationEvent {
        private final UUID reportId;
        private final String orgId;
        private final String userId;
        private final long timestamp;

        public LocalReleasedInternalEvent(Object source, UUID reportId, String orgId, String userId) {
            super(source);
            this.reportId = reportId;
            this.orgId = orgId;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getReportId() { return reportId; }
        public String getOrgId() { return orgId; }
        public String getUserId() { return userId; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Event fired when report data is locked after approval.
     */
    public static class DataLockedInternalEvent extends org.springframework.context.ApplicationEvent {
        private final UUID reportId;
        private final String orgId;
        private final String userId;
        private final long timestamp;

        public DataLockedInternalEvent(Object source, UUID reportId, String orgId, String userId) {
            super(source);
            this.reportId = reportId;
            this.orgId = orgId;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getReportId() { return reportId; }
        public String getOrgId() { return orgId; }
        public String getUserId() { return userId; }
        public long getTimestamp() { return timestamp; }
    }
}
