package com.team14.amazon.product.messaging;

import com.team14.amazon.contracts.events.ProductDiscontinuedEvent;
import com.team14.amazon.contracts.events.ProductRatedEvent;
import com.team14.amazon.contracts.events.ProductReviewAddedEvent;
import com.team14.amazon.product.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ProductEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishProductDiscontinued(Long productId, String oldStatus, String newStatus) {
        var event = new ProductDiscontinuedEvent(productId, oldStatus, newStatus);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, "product.discontinued", event);
        log.info("Published product.discontinued for productId={}", productId);
    }

    public void publishProductReviewAdded(Long productId, Long reviewId, Long userId, Integer rating) {
        var event = new ProductReviewAddedEvent(productId, reviewId, userId, rating);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, "product.review-added", event);
        log.info("Published product.review-added for productId={}", productId);
    }

    public void publishProductRated(Long productId, Double newAverage, Integer totalRatings) {
        var event = new ProductRatedEvent(productId, newAverage, totalRatings);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, "product.rated", event);
        log.info("Published product.rated for productId={}", productId);
    }
}
