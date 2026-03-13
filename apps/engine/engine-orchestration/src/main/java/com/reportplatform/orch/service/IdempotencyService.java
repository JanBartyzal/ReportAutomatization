package com.reportplatform.orch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based idempotency service.
 * <p>
 * Prevents duplicate processing of the same file at the same workflow step
 * by caching step results keyed by {@code file_id:step_hash}.
 * </p>
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public IdempotencyService(StringRedisTemplate redisTemplate,
                              @Value("${workflow.idempotency.ttl-hours:24}") int ttlHours) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofHours(ttlHours);
    }

    /**
     * Checks if a step has already been processed for the given file.
     *
     * @param fileId   the file identifier
     * @param stepHash a hash representing the specific workflow step
     * @return the cached result if present, empty otherwise
     */
    public Optional<String> checkProcessed(String fileId, String stepHash) {
        String key = buildKey(fileId, stepHash);
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("Idempotency hit for key [{}]", key);
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * Marks a step as processed and caches its result.
     *
     * @param fileId   the file identifier
     * @param stepHash a hash representing the specific workflow step
     * @param result   the serialized result to cache
     */
    public void markProcessed(String fileId, String stepHash, String result) {
        String key = buildKey(fileId, stepHash);
        redisTemplate.opsForValue().set(key, result, ttl);
        log.debug("Idempotency set for key [{}] with TTL {}h", key, ttl.toHours());
    }

    /**
     * Removes the idempotency entry, allowing reprocessing.
     *
     * @param fileId   the file identifier
     * @param stepHash a hash representing the specific workflow step
     */
    public void invalidate(String fileId, String stepHash) {
        String key = buildKey(fileId, stepHash);
        redisTemplate.delete(key);
        log.debug("Idempotency invalidated for key [{}]", key);
    }

    private String buildKey(String fileId, String stepHash) {
        return KEY_PREFIX + fileId + ":" + stepHash;
    }
}
