package com.reportplatform.qry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache wrapper for query results.
 * Keys are structured as: qry:{org_id}:{entity}:{id}
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String KEY_PREFIX = "qry";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;

    public CacheService(RedisTemplate<String, Object> redisTemplate,
                        ObjectMapper objectMapper,
                        @Value("${cache.ttl-minutes:5}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultTtl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * Build a structured cache key.
     */
    public String buildKey(String orgId, String entity, String id) {
        return KEY_PREFIX + ":" + orgId + ":" + entity + ":" + id;
    }

    /**
     * Get a cached value, deserialized to the given type.
     */
    public <T> Optional<T> getCached(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            // Fallback: serialize and deserialize for type safety
            String json = objectMapper.writeValueAsString(value);
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached value for key: {}", key, e);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis get failed for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Put a value into the cache with the default TTL.
     */
    public void putCache(String key, Object value) {
        putCache(key, value, defaultTtl);
    }

    /**
     * Put a value into the cache with a custom TTL.
     */
    public void putCache(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Redis put failed for key: {}", key, e);
        }
    }

    /**
     * Evict a cached value by key.
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis evict failed for key: {}", key, e);
        }
    }

    /**
     * Evict all cache entries for a given org and entity type.
     * Uses pattern-based deletion.
     */
    public void evictByPattern(String orgId, String entity) {
        try {
            String pattern = KEY_PREFIX + ":" + orgId + ":" + entity + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} cache entries for pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Redis pattern evict failed for org: {}, entity: {}", orgId, entity, e);
        }
    }
}
