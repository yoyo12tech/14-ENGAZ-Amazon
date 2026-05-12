package com.team14.amazon.shipping.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String SHIPMENT_EXCHANGE = "shipment.events";
    public static final String ORDER_EXCHANGE = "order.events";
    public static final String SHIPPING_ORDER_QUEUE = "shipping.order.queue";
    public static final String SHIPPING_ORDER_DLQ = "shipping.order.dlq";

    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange(SHIPMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue shippingOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", "shipping.order.dlq");
        return new Queue(SHIPPING_ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue shippingOrderDlq() {
        return new Queue(SHIPPING_ORDER_DLQ);
    }

    @Bean
    public Binding bindOrderPlaced(Queue shippingOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shippingOrderQueue).to(orderExchange).with("order.placed");
    }

    @Bean
    public Binding bindOrderCompleted(Queue shippingOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shippingOrderQueue).to(orderExchange).with("order.completed");
    }

    @Bean
    public Binding bindOrderCancelled(Queue shippingOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shippingOrderQueue).to(orderExchange).with("order.cancelled");
    }
}
