package com.team14.amazon.user.observer;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.HashSet;

@Component
public class RedisCacheObserver implements EntityObserver {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheObserver(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onEvent(String action, Object payload) {
        if ("ROLE_CHANGED".equals(action)) {
            invalidateUserCaches();
        }
    }

    private void invalidateUserCaches() {
        // Pattern for user activity feed as per section 9.2 (f) and 4.4.4
        String pattern = "user-service::S1-F12::*";
        
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            Set<String> keysToDelete = new HashSet<>();
            while (cursor.hasNext()) {
                keysToDelete.add(new String(cursor.next()));
            }
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (Exception e) {
            // Log error but don't break the main flow
            System.err.println("Failed to invalidate Redis cache: " + e.getMessage());
        }
    }
}
