package com.team14.amazon.product.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_EXCHANGE = "product.events";
    public static final String ORDER_EXCHANGE = "order.events";
    public static final String PRODUCT_ORDER_QUEUE = "product.order.queue";
    public static final String PRODUCT_ORDER_DLQ = "product.order.dlq";

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue productOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", "product.order.dlq");
        return new Queue(PRODUCT_ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue productOrderDlq() {
        return new Queue(PRODUCT_ORDER_DLQ);
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
}
