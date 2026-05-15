package com.team14.amazon.billing.messaging;

import com.team14.amazon.contracts.events.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishInitiated(PaymentInitiatedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.initiated", event);
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.completed", event);
    }

    public void publishFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.failed", event);
    }

    public void publishRefunded(PaymentRefundedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.refunded", event);
    }
}

