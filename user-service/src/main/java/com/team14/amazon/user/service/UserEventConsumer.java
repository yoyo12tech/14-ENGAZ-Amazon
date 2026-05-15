package com.team14.amazon.user.service;

import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);
    private final UserRepository userRepository;

    public UserEventConsumer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "user.order.saga-listener")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received order.completed event for orderId: {} and userId: {}",
                event.orderId(), event.userId());
    }

    @RabbitListener(queues = "user.order.saga-listener")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled event for orderId: {} and userId: {}",
                event.orderId(), event.userId());
    }
}