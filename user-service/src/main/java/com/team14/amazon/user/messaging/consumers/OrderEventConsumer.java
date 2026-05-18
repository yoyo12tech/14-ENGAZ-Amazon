package com.team14.amazon.user.messaging.consumers;

import com.team14.amazon.user.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Consumes order.completed and order.cancelled from order.events exchange.
 *
 * Action: wildcard-invalidate Redis cache keys for S1-F3, S1-F6, S1-F9.
 * Idempotency: Approach 1 — replay is harmless (cache miss triggers fresh Feign call).
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
    public void handleOrderEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String corrId = message.getMessageProperties().getHeader("X-Correlation-ID");
        if (corrId != null) MDC.put("correlationId", corrId);
        if (routingKey != null) MDC.put("routingKey", routingKey);

        try {
            log.info("Consuming {} — invalidating user-service caches", routingKey);
            invalidatePattern(F3_PATTERN);
            invalidatePattern(F6_PATTERN);
            invalidatePattern(F9_PATTERN);
            log.info("Processed {} — cache invalidation complete", routingKey);
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
            MDC.remove("routingKey");
        }
    }

    private void invalidatePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching '{}'", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache invalidation failed for pattern '{}': {}", pattern, e.getMessage());
        }
    }
}
