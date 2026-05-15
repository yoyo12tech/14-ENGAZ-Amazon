package com.team14.amazon.billing.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentEventConfig {

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange("payment.events");
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
    public Binding orderCompletedBinding() {
        return BindingBuilder.bind(paymentSagaListenerQueue())
                .to(new TopicExchange("order.events"))
                .with("order.completed");
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(paymentSagaListenerQueue())
                .to(new TopicExchange("order.events"))
                .with("order.cancelled");
    }
}

