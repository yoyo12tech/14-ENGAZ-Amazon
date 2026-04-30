package com.team14.amazon.product.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);
    private final StringRedisTemplate redisTemplate;

    public CacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Cache evict failed [{}]: {}", key, e.getMessage());
        }
    }

    // M2 4.4.6 - wildcard deletion is the required strategy
    public void evictByPattern(String pattern) {
        try {
            ScanOptions opts = ScanOptions.scanOptions().match(pattern).count(200).build();
            Set<String> keys = new HashSet<>();
            try (Cursor<byte[]> cursor =
                         redisTemplate.getConnectionFactory().getConnection().scan(opts)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching '{}'", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache wildcard evict failed [{}]: {}", pattern, e.getMessage());
        }
    }

    // Called on every product write (CRUD + S2-F2/F4/F7/F8)
    // M2 4.4.4 - must invalidate detail + all feature caches referencing this entity
    public void invalidateProductCaches(Long productId) {
        evict("product-service::product::" + productId);
        evictByPattern("product-service::S2-F1::*");
        evictByPattern("product-service::S2-F3::*");
        evictByPattern("product-service::S2-F5::*");
        evictByPattern("product-service::S2-F6::*");
        evictByPattern("product-service::S2-F9::*");
        // M2 new features - must also be invalidated (4.4.4)
        evictByPattern("product-service::S2-F10::*");
        evictByPattern("product-service::S2-F12::*");
    }

    // Called on review writes (S2-F7, S2-F8, review CRUD PUT/DELETE)
    // Rating change -> product detail stale -> cascade to product caches
    public void invalidateReviewCaches(Long reviewId, Long productId) {
        if (reviewId != null) {
            evict("product-service::product-review::" + reviewId);
        }
        if (productId != null) {
            invalidateProductCaches(productId);
        }
    }
}


