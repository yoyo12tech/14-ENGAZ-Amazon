package com.team14.amazon.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.events";
    public static final String SHIPMENT_EXCHANGE = "shipment.events";
    public static final String PAYMENT_EXCHANGE = "payment.events";
    
    public static final String ORDER_SHIPMENT_QUEUE = "order.shipment.queue";
    public static final String ORDER_PAYMENT_QUEUE = "order.payment.queue";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange(SHIPMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue orderShipmentQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SHIPMENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.shipment.dlq");
        return new Queue(ORDER_SHIPMENT_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue orderShipmentDlq() {
        return new Queue("order.shipment.dlq");
    }

    @Bean
    public Queue orderPaymentQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", PAYMENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.payment.dlq");
        return new Queue(ORDER_PAYMENT_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue orderPaymentDlq() {
        return new Queue("order.payment.dlq");
    }

    @Bean
    public Binding bindShipmentCreated(Queue orderShipmentQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(orderShipmentQueue).to(shipmentExchange).with("shipment.created");
    }

    @Bean
    public Binding bindShipmentStatusChanged(Queue orderShipmentQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(orderShipmentQueue).to(shipmentExchange).with("shipment.status-changed");
    }

    @Bean
    public Binding bindPaymentInitiated(Queue orderPaymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentQueue).to(paymentExchange).with("payment.initiated");
    }

    @Bean
    public Binding bindPaymentCompleted(Queue orderPaymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentQueue).to(paymentExchange).with("payment.completed");
    }

    @Bean
    public Binding bindPaymentFailed(Queue orderPaymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentQueue).to(paymentExchange).with("payment.failed");
    }

    @Bean
    public Binding bindPaymentRefunded(Queue orderPaymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentQueue).to(paymentExchange).with("payment.refunded");
    }
}
