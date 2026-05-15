package com.team14.amazon.user.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String USER_EXCHANGE  = "user.events";
    public static final String ORDER_EXCHANGE = "order.events";

    // Queue names (spec-mandated)
    public static final String USER_ORDER_SAGA_QUEUE = "user.order.saga-listener";
    public static final String USER_ORDER_SAGA_DLQ   = "user.order.saga-listener.dlq";

    // ── Exchanges ──────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    // ── DLQ (declared first — main queue references it) ───────────────────────

    @Bean
    public Queue userOrderSagaDlq() {
        // Plain durable queue; dead-lettered messages land here via default exchange
        return QueueBuilder.durable(USER_ORDER_SAGA_DLQ).build();
    }

    // ── Main consumer queue ────────────────────────────────────────────────────

    @Bean
    public Queue userOrderSagaQueue() {
        Map<String, Object> args = new HashMap<>();
        // Route rejected/expired messages to the DLQ via the default exchange
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", USER_ORDER_SAGA_DLQ);
        return new Queue(USER_ORDER_SAGA_QUEUE, true, false, false, args);
    }

    // ── Bindings: saga queue listens to order.events ──────────────────────────

    @Bean
    public Binding bindOrderCompleted() {
        return BindingBuilder
                .bind(userOrderSagaQueue())
                .to(orderExchange())
                .with("order.completed");
    }

    @Bean
    public Binding bindOrderCancelled() {
        return BindingBuilder
                .bind(userOrderSagaQueue())
                .to(orderExchange())
                .with("order.cancelled");
    }
}