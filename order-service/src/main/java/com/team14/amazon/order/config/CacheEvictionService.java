package com.team14.amazon.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Wildcard cache eviction helper (Section 4.4.6).
 * Uses Redis SCAN + DEL pattern matching.
 */
@Service
public class CacheEvictionService {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheEvictionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Delete a single specific key. */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[CacheEviction] Failed to evict key '{}': {}", key, e.getMessage());
        }
    }

    /** Delete all keys matching the given pattern (e.g., "order-service::S3-F10::*"). */
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("[CacheEviction] Evicted {} keys matching '{}'", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("[CacheEviction] Failed to evict pattern '{}': {}", pattern, e.getMessage());
        }
    }

    // Convenience methods for order-service cache keys

    public void evictOrderDetail(Long orderId) {
        evict("order-service::order::" + orderId);
    }

    public void evictOrderItemDetail(Long itemId) {
        evict("order-service::order-item::" + itemId);
    }

    /** Invalidate all S3-F1 search results (pattern). */
    public void evictOrderFeatureCaches() {
        evictByPattern("order-service::S3-F1::*");
        evictByPattern("order-service::S3-F3::*");
        evictByPattern("order-service::S3-F5::*");
        evictByPattern("order-service::S3-F6::*");
        evictByPattern("order-service::S3-F9::*");
        evictByPattern("order-service::S3-F10::*");
    }

    /** Invalidate S3-F12 recommendation cache (after S3-F11 co-purchase update). */
    public void evictRecommendationCache() {
        evictByPattern("order-service::S3-F12::*");
    }
}
