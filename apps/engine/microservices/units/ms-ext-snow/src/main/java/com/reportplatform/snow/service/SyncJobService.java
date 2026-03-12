package com.reportplatform.snow.service;

import com.reportplatform.snow.model.entity.SyncJobHistoryEntity;
import com.reportplatform.snow.model.entity.SyncJobHistoryEntity.JobStatus;
import com.reportplatform.snow.model.entity.SyncScheduleEntity;
import com.reportplatform.snow.model.entity.SyncScheduleEntity.SyncStatus;
import com.reportplatform.snow.repository.SyncJobHistoryRepository;
import com.reportplatform.snow.repository.SyncScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class SyncJobService {

    private static final Logger logger = LoggerFactory.getLogger(SyncJobService.class);

    private final SyncScheduleRepository syncScheduleRepository;
    private final SyncJobHistoryRepository syncJobHistoryRepository;
    private final DataFetchService dataFetchService;

    public SyncJobService(SyncScheduleRepository syncScheduleRepository,
                          SyncJobHistoryRepository syncJobHistoryRepository,
                          DataFetchService dataFetchService) {
        this.syncScheduleRepository = syncScheduleRepository;
        this.syncJobHistoryRepository = syncJobHistoryRepository;
        this.dataFetchService = dataFetchService;
    }

    /**
     * Execute a sync for the given schedule.
     * Updates the schedule status, creates a job history record, invokes data fetch,
     * and updates all records with the result.
     */
    @Transactional
    public void executeSync(SyncScheduleEntity schedule) {
        logger.info("Starting sync execution for schedule: {} (connection: {})",
                schedule.getId(), schedule.getConnectionId());

        // Step 1: Set schedule status to RUNNING
        schedule.setStatus(SyncStatus.RUNNING);
        syncScheduleRepository.save(schedule);

        // Step 2: Create job history record
        SyncJobHistoryEntity job = new SyncJobHistoryEntity();
        job.setScheduleId(schedule.getId());
        job.setOrgId(schedule.getOrgId());
        job.setStartedAt(Instant.now());
        job.setStatus(JobStatus.RUNNING);
        job = syncJobHistoryRepository.save(job);

        try {
            // Step 3: Fetch and store data
            DataFetchService.FetchResult result = dataFetchService.fetchAndStore(
                    schedule.getConnectionId(), schedule.getLastSyncTimestamp());

            // Step 4: Update job history with result counts
            job.setRecordsFetched(result.getRecordsFetched());
            job.setRecordsStored(result.getRecordsStored());
            job.setCompletedAt(Instant.now());
            job.setStatus(JobStatus.COMPLETED);
            syncJobHistoryRepository.save(job);

            // Step 5: Update schedule
            Instant now = Instant.now();
            schedule.setLastRunAt(now);
            schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
            schedule.setLastSyncTimestamp(now.toString());
            schedule.setStatus(SyncStatus.IDLE);
            syncScheduleRepository.save(schedule);

            logger.info("Sync completed for schedule: {}. Fetched: {}, Stored: {}",
                    schedule.getId(), result.getRecordsFetched(), result.getRecordsStored());

            // TODO: Publish event to Dapr Pub/Sub (snow.sync.completed)
            // daprClient.publishEvent(pubsubName, "snow.sync.completed", eventPayload);

        } catch (Exception ex) {
            logger.error("Sync failed for schedule: {}", schedule.getId(), ex);

            // Update job history with failure
            job.setCompletedAt(Instant.now());
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
            syncJobHistoryRepository.save(job);

            // Update schedule status to FAILED
            schedule.setStatus(SyncStatus.FAILED);
            schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
            syncScheduleRepository.save(schedule);

            // TODO: Publish event to Dapr Pub/Sub (snow.sync.failed)
            // daprClient.publishEvent(pubsubName, "snow.sync.failed", eventPayload);
        }
    }

    /**
     * Calculate the next run time from a Spring cron expression.
     * Uses UTC timezone for consistency.
     */
    public Instant calculateNextRun(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime next = cron.next(now);
            if (next != null) {
                return next.toInstant(ZoneOffset.UTC);
            }
            logger.warn("Cron expression '{}' returned no next execution time.", cronExpression);
            return null;
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid cron expression: '{}'", cronExpression, ex);
            return null;
        }
    }
}
