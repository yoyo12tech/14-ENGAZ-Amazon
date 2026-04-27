package com.team14.amazon.billing.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
public class BillingCacheInvalidationService {

    private final CacheManager cacheManager;
    private final StringRedisTemplate redisTemplate;

    public BillingCacheInvalidationService(CacheManager cacheManager, StringRedisTemplate redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    public void evictTransactionCaches(Long transactionId) {
        evictEntityCache(BillingCacheNames.TRANSACTION, transactionId);
        evictFeatureCaches();
    }

    public void evictVoucherCaches(Long voucherId) {
        evictEntityCache(BillingCacheNames.VOUCHER, voucherId);
        evictFeatureCaches();
    }

    public void evictTransactionVoucherCaches(Long transactionVoucherId) {
        evictEntityCache(BillingCacheNames.TRANSACTION_VOUCHER, transactionVoucherId);
        evictFeatureCaches();
    }

    public void evictTransactionAndVoucherCaches(Long transactionId, Long voucherId) {
        evictEntityCache(BillingCacheNames.TRANSACTION, transactionId);
        evictEntityCache(BillingCacheNames.VOUCHER, voucherId);
        evictFeatureCaches();
    }

    private void evictEntityCache(String cacheName, Long id) {
        if (id == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(BillingCacheNames.entityKey(id));
        }
    }

    private void evictFeatureCaches() {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory == null) {
            return;
        }

        Set<String> keysToDelete = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(BillingCacheNames.S5_FEATURE_PATTERN).count(100).build();

        try (RedisConnection connection = connectionFactory.getConnection();
             Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                keysToDelete.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }

        if (!keysToDelete.isEmpty()) {
            redisTemplate.unlink(keysToDelete);
        }
    }
}

