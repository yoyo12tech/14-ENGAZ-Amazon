package com.team14.amazon.user.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    public static final String USER_EXCHANGE = "user.events";
    public static final String ORDER_EXCHANGE = "order.events";
    public static final String USER_ORDER_QUEUE = "user.order.queue";
    public static final String USER_ORDER_DLQ = "user.order.dlq";

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue userOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", "user.order.dlq");
        return new Queue(USER_ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue userOrderDlq() {
        return new Queue(USER_ORDER_DLQ);
    }

    @Bean
    public Binding bindOrderCompleted(Queue userOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(userOrderQueue).to(orderExchange).with("order.completed");
    }

    @Bean
    public Binding bindOrderCancelled(Queue userOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(userOrderQueue).to(orderExchange).with("order.cancelled");
    }
}
