package com.team14.amazon.product.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_EXCHANGE    = "product.events";
    public static final String ORDER_EXCHANGE      = "order.events";
    public static final String PRODUCT_ORDER_QUEUE = "product.order.saga-listener";
    public static final String PRODUCT_ORDER_DLQ   = "product.order.saga-listener.dlq";
    public static final String PRODUCT_DLX         = "product.dlx";

    // ── Exchanges ────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    // Separate dead-letter exchange — NOT order.events
    @Bean
    public DirectExchange productDeadLetterExchange() {
        return new DirectExchange(PRODUCT_DLX);
    }

    // ── Queues ───────────────────────────────────────────────────────────────

    @Bean
    public Queue productOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", PRODUCT_DLX);
        args.put("x-dead-letter-routing-key", PRODUCT_ORDER_DLQ);
        return new Queue(PRODUCT_ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue productOrderDlq() {
        return new Queue(PRODUCT_ORDER_DLQ, true);
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding dlqBinding(Queue productOrderDlq, DirectExchange productDeadLetterExchange) {
        return BindingBuilder.bind(productOrderDlq)
                .to(productDeadLetterExchange)
                .with(PRODUCT_ORDER_DLQ);
    }

    @Bean
    public Binding bindOrderPlaced(Queue productOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(productOrderQueue).to(orderExchange).with("order.placed");
    }

    @Bean
    public Binding bindOrderCompleted(Queue productOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(productOrderQueue).to(orderExchange).with("order.completed");
    }

    @Bean
    public Binding bindOrderCancelled(Queue productOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(productOrderQueue).to(orderExchange).with("order.cancelled");
    }

    // ── Jackson converter for outbound publishing ────────────────────────────

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
