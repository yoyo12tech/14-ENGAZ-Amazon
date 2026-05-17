package com.team14.amazon.order.messaging;

import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.events.OrderPlacedEvent;
import com.team14.amazon.order.config.OrderEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes the three order domain events to order.events TopicExchange.
 *
 * Lives in messaging/ package per spec §12 folder tree.
 *
 * Spec §11.1 required log points:
 *   INFO  "Published {routingKey} for orderId={id}"   on success
 *   ERROR "Failed to publish {routingKey}: {message}" on exception
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        send(OrderEventConfig.ROUTING_ORDER_PLACED, event, event.orderId());
    }

    public void publishOrderCompleted(OrderCompletedEvent event) {
        send(OrderEventConfig.ROUTING_ORDER_COMPLETED, event, event.orderId());
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        send(OrderEventConfig.ROUTING_ORDER_CANCELLED, event, event.orderId());
    }

    private void send(String routingKey, Object payload, Long orderId) {
        MDC.put("routingKey", routingKey);
        MDC.put("orderId", orderId != null ? orderId.toString() : "null");
        try {
            rabbitTemplate.convertAndSend(
                    OrderEventConfig.ORDER_EXCHANGE,
                    routingKey,
                    payload);
            log.info("Published {} for orderId={}", routingKey, orderId);
        } catch (Exception ex) {
            log.error("Failed to publish {} for orderId={}: {}",
                    routingKey, orderId, ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("routingKey");
            MDC.remove("orderId");
        }
    }
}