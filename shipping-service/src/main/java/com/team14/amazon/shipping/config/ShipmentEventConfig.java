package com.team14.amazon.shipping.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class ShipmentEventConfig {

    @Bean
    public TopicExchange shipmentEventsExchange() {
        return new TopicExchange("shipment.events");
    }

    @Bean
    public DirectExchange shipmentDlx() {
        return new DirectExchange("shipment.dlx");
    }

    @Bean
    public Queue shipmentSagaListenerDlq() {
        return QueueBuilder.durable("shipment.saga-listener.dlq").build();
    }

    @Bean
    public Binding shipmentSagaListenerDlqBinding() {
        return BindingBuilder.bind(shipmentSagaListenerDlq())
                .to(shipmentDlx())
                .with("shipment.saga-listener.dlq");
    }

    @Bean
    public Queue shipmentSagaListenerQueue() {
        return QueueBuilder.durable("shipment.saga-listener")
                .withArgument("x-dead-letter-exchange", "shipment.dlx")
                .withArgument("x-dead-letter-routing-key", "shipment.saga-listener.dlq")
                .build();
    }

    @Bean
    public Binding orderPlacedBinding() {
        return BindingBuilder.bind(shipmentSagaListenerQueue())
                .to(new TopicExchange("order.events"))
                .with("order.placed");
    }

    @Bean
    public Binding orderCompletedBinding() {
        return BindingBuilder.bind(shipmentSagaListenerQueue())
                .to(new TopicExchange("order.events"))
                .with("order.completed");
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(shipmentSagaListenerQueue())
                .to(new TopicExchange("order.events"))
                .with("order.cancelled");
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