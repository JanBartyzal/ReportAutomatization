package com.reportplatform.snow.service;

import com.reportplatform.snow.model.entity.SyncScheduleEntity;
import com.reportplatform.snow.model.entity.SyncScheduleEntity.SyncStatus;
import com.reportplatform.snow.repository.SyncScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SyncSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SyncSchedulerService.class);

    private static final String LOCK_KEY_PREFIX = "snow:sync:lock:";

    private final SyncScheduleRepository syncScheduleRepository;
    private final SyncJobService syncJobService;
    private final StringRedisTemplate stringRedisTemplate;
    private final long lockTtlMs;

    public SyncSchedulerService(SyncScheduleRepository syncScheduleRepository,
                                SyncJobService syncJobService,
                                StringRedisTemplate stringRedisTemplate,
                                @Value("${servicenow.scheduler.lock-ttl-ms:300000}") long lockTtlMs) {
        this.syncScheduleRepository = syncScheduleRepository;
        this.syncJobService = syncJobService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockTtlMs = lockTtlMs;
    }

    /**
     * Polls for due sync schedules at a configurable fixed delay.
     * For each due schedule, attempts to acquire a Redis distributed lock
     * before executing the sync to prevent duplicate runs across instances.
     */
    @Scheduled(fixedDelayString = "${servicenow.scheduler.poll-interval-ms:60000}")
    public void pollSchedules() {
        logger.debug("Polling for due sync schedules...");

        Instant now = Instant.now();
        List<SyncScheduleEntity> dueSchedules = syncScheduleRepository
                .findByEnabledTrueAndStatusAndNextRunAtBefore(SyncStatus.IDLE, now);

        if (dueSchedules.isEmpty()) {
            logger.debug("No due schedules found.");
            return;
        }

        logger.info("Found {} due schedule(s) to process.", dueSchedules.size());

        for (SyncScheduleEntity schedule : dueSchedules) {
            String lockKey = LOCK_KEY_PREFIX + schedule.getId();
            boolean lockAcquired = false;

            try {
                lockAcquired = acquireLock(lockKey);
                if (!lockAcquired) {
                    logger.debug("Could not acquire lock for schedule: {}. Skipping (another instance may be processing it).",
                            schedule.getId());
                    continue;
                }

                logger.info("Lock acquired for schedule: {}. Executing sync.", schedule.getId());
                syncJobService.executeSync(schedule);

            } catch (Exception ex) {
                logger.error("Error processing schedule: {}", schedule.getId(), ex);
            } finally {
                if (lockAcquired) {
                    releaseLock(lockKey);
                    logger.debug("Lock released for schedule: {}", schedule.getId());
                }
            }
        }
    }

    private boolean acquireLock(String lockKey) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofMillis(lockTtlMs));
        return Boolean.TRUE.equals(result);
    }

    private void releaseLock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
        } catch (Exception ex) {
            logger.warn("Failed to release lock for key: {}. It will expire after TTL.", lockKey, ex);
        }
    }
}
