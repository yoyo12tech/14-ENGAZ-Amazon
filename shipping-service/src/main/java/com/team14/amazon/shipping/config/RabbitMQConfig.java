/*
package com.team14.amazon.shipping.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String SHIPMENT_EXCHANGE = "shipment.events";
    public static final String ORDER_EXCHANGE    = "order.events";
    public static final String SAGA_QUEUE        = "shipment.saga-listener";
    public static final String SAGA_DLQ          = "shipment.saga-listener.dlq";
    public static final String SAGA_DLX          = "shipment.saga-listener.dlx";

    @Bean public TopicExchange shipmentExchange() { return new TopicExchange(SHIPMENT_EXCHANGE); }
    @Bean public TopicExchange orderExchange()    { return new TopicExchange(ORDER_EXCHANGE); }

    @Bean public DirectExchange sagaDlx() { return new DirectExchange(SAGA_DLX); }

    @Bean public Binding dlqBinding(Queue shipmentSagaDlq, DirectExchange sagaDlx) {
        return BindingBuilder.bind(shipmentSagaDlq).to(sagaDlx).with(SAGA_DLQ);
    }

    @Bean
    public Queue shipmentSagaQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SAGA_DLX);
        args.put("x-dead-letter-routing-key", SAGA_DLQ);
        return new Queue(SAGA_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue shipmentSagaDlq() {
        return new Queue(SAGA_DLQ, true);
    }

    @Bean public Binding bindOrderPlaced(Queue shipmentSagaQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shipmentSagaQueue).to(orderExchange).with("order.placed");
    }
    @Bean public Binding bindOrderCompleted(Queue shipmentSagaQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shipmentSagaQueue).to(orderExchange).with("order.completed");
    }
    @Bean public Binding bindOrderCancelled(Queue shipmentSagaQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(shipmentSagaQueue).to(orderExchange).with("order.cancelled");
    }

    @Bean
    public MessageConverter jackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jackson2MessageConverter());
        return t;
    }
}

 */