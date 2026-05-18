package com.team14.amazon.billing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentEventConfig {

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange("payment.events");
    }

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange("order.events");
    }

    @Bean
    public DirectExchange paymentDlx() {
        return new DirectExchange("payment.dlx");
    }

    @Bean
    public Queue paymentSagaListenerDlq() {
        return QueueBuilder.durable("payment.saga-listener.dlq").build();
    }

    @Bean
    public Binding paymentSagaListenerDlqBinding() {
        return BindingBuilder.bind(paymentSagaListenerDlq())
                .to(paymentDlx())
                .with("payment.saga-listener.dlq");
    }

    @Bean
    public Queue paymentSagaListenerQueue() {
        return QueueBuilder.durable("payment.saga-listener")
                .withArgument("x-dead-letter-exchange", "payment.dlx")
                .withArgument("x-dead-letter-routing-key", "payment.saga-listener.dlq")
                .build();
    }

    @Bean
    public Binding orderCompletedBinding(Queue paymentSagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(paymentSagaListenerQueue)
                .to(orderEventsExchange)
                .with("order.completed");
    }

    @Bean
    public Binding orderCancelledBinding(Queue paymentSagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(paymentSagaListenerQueue)
                .to(orderEventsExchange)
                .with("order.cancelled");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
