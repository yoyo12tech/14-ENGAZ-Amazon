package com.team14.amazon.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Consumes order.completed and order.cancelled from order.events exchange.
 *
 * On either event, wildcard-invalidates the Redis cache keys for:
 *   - S1-F3  user-service::S1-F3::{userId}::*
 *   - S1-F6  user-service::S1-F6::*
 *   - S1-F9  user-service::S1-F9::*
 *
 * The User entity is never mutated — the next Feign read recomputes live aggregates.
 * Idempotency: Approach 1 — replay is harmless (cache miss just triggers a fresh Feign call).
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final String F3_PATTERN = "user-service::S1-F3::*";
    private static final String F6_PATTERN = "user-service::S1-F6::*";
    private static final String F9_PATTERN = "user-service::S1-F9::*";

    private final StringRedisTemplate redisTemplate;

    public OrderEventConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.USER_ORDER_SAGA_QUEUE)
    public void handleOrderEvent(@Payload(required = false) String payload) {
        log.info("Order event received — invalidating user-service caches");
        invalidatePattern(F3_PATTERN);
        invalidatePattern(F6_PATTERN);
        invalidatePattern(F9_PATTERN);
    }

    private void invalidatePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching '{}'", keys.size(), pattern);
            }
        } catch (Exception e) {
            // Redis is a soft dependency — log and continue
            log.warn("Cache invalidation failed for pattern '{}': {}", pattern, e.getMessage());
        }
    }
}