package com.team14.amazon.product.messaging.publishers;

import com.team14.amazon.contracts.events.ProductDiscontinuedEvent;
import com.team14.amazon.contracts.events.ProductRatedEvent;
import com.team14.amazon.contracts.events.ProductReviewAddedEvent;
import com.team14.amazon.product.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
        String routingKey = "product.discontinued";
        MDC.put("routingKey", routingKey);
        MDC.put("productId", productId.toString());
        try {
            var event = new ProductDiscontinuedEvent(productId, oldStatus, newStatus);
            rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, routingKey, event);
            log.info("Published {} for productId={}", routingKey, productId);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("productId");
        }
    }

    public void publishProductReviewAdded(Long productId, Long reviewId, Long userId, Integer rating) {
        String routingKey = "product.review-added";
        MDC.put("routingKey", routingKey);
        MDC.put("productId", productId.toString());
        try {
            var event = new ProductReviewAddedEvent(productId, reviewId, userId, rating);
            rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, routingKey, event);
            log.info("Published {} for productId={}", routingKey, productId);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("productId");
        }
    }

    public void publishProductRated(Long productId, Double newAverage, Integer totalRatings) {
        String routingKey = "product.rated";
        MDC.put("routingKey", routingKey);
        MDC.put("productId", productId.toString());
        try {
            var event = new ProductRatedEvent(productId, newAverage, totalRatings);
            rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, routingKey, event);
            log.info("Published {} for productId={}", routingKey, productId);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("productId");
        }
    }
}
