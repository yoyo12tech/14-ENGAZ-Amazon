package com.team14.amazon.billing.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.events";
    public static final String ORDER_EXCHANGE = "order.events";
    public static final String BILLING_ORDER_QUEUE = "billing.order.queue";
    public static final String BILLING_ORDER_DLQ = "billing.order.dlq";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue billingOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", "billing.order.dlq");
        return new Queue(BILLING_ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue billingOrderDlq() {
        return new Queue(BILLING_ORDER_DLQ);
    }

    @Bean
    public Binding bindOrderCompleted(Queue billingOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(billingOrderQueue).to(orderExchange).with("order.completed");
    }

    @Bean
    public Binding bindOrderCancelled(Queue billingOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(billingOrderQueue).to(orderExchange).with("order.cancelled");
    }
}
