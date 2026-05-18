package com.team14.amazon.billing.messaging.publishers;

import com.team14.amazon.contracts.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishInitiated(PaymentInitiatedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.initiated", event);
        log.info("Published {} for {}={}", "payment.initiated", "transaction", event.transactionId());
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.completed", event);
        log.info("Published {} for {}={}", "payment.completed", "transaction", event.transactionId());
    }

    public void publishFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.failed", event);
        log.info("Published {} for {}={}", "payment.failed", "transaction", event.transactionId());
    }

    public void publishRefunded(PaymentRefundedEvent event) {
        rabbitTemplate.convertAndSend("payment.events", "payment.refunded", event);
        log.info("Published {} for {}={}", "payment.refunded", "transaction", event.transactionId());
    }
}
