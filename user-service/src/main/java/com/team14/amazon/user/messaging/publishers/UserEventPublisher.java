package com.team14.amazon.user.messaging.publishers;

import com.team14.amazon.contracts.events.UserDeactivatedEvent;
import com.team14.amazon.contracts.events.UserRegisteredEvent;
import com.team14.amazon.user.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.registered", event);
        log.info("Published user.registered for userId={}", event.userId());
    }

    public void publishUserDeactivated(UserDeactivatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.deactivated", event);
        log.info("Published user.deactivated for userId={}", event.userId());
    }
}
