package com.reportplatform.excelsync.service;

import com.reportplatform.excelsync.config.ExcelSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class ConcurrencyGuard {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyGuard.class);
    private static final String LOCK_KEY_PREFIX = "excel-sync:lock:";

    private final StringRedisTemplate redisTemplate;
    private final ExcelSyncProperties properties;

    public ConcurrencyGuard(StringRedisTemplate redisTemplate, ExcelSyncProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public String tryAcquire(UUID flowId) {
        String lockKey = LOCK_KEY_PREFIX + flowId;
        String lockValue = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMillis(properties.getLockTtlMs());

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            log.info("Acquired lock for flow [{}]", flowId);
            return lockValue;
        }
        log.warn("Failed to acquire lock for flow [{}] - concurrent execution in progress", flowId);
        return null;
    }

    public void release(UUID flowId, String lockValue) {
        String lockKey = LOCK_KEY_PREFIX + flowId;
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
            log.info("Released lock for flow [{}]", flowId);
        } else {
            log.warn("Lock for flow [{}] has been overridden (TTL expired?)", flowId);
        }
    }
}
