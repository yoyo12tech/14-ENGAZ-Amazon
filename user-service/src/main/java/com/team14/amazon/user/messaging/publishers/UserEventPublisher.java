package com.team14.amazon.user.messaging.publishers;

import com.team14.amazon.contracts.events.UserDeactivatedEvent;
import com.team14.amazon.contracts.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "user.events";

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing user.registered event for userId: {}", event.userId());
        rabbitTemplate.convertAndSend(EXCHANGE, "user.registered", event);
    }

    public void publishUserDeactivated(UserDeactivatedEvent event) {
        log.info("Publishing user.deactivated event for userId: {}", event.userId());
        rabbitTemplate.convertAndSend(EXCHANGE, "user.deactivated", event);
    }
}
