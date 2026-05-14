package com.team14.amazon.product.service;

import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.events.OrderItemPayload;
import com.team14.amazon.contracts.events.OrderPlacedEvent;
import com.team14.amazon.product.config.RabbitMQConfig;
import com.team14.amazon.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final ProductRepository productRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public OrderEventListener(ProductRepository productRepository, CacheInvalidationService cacheInvalidationService) {
        this.productRepository = productRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_ORDER_QUEUE)
    @Transactional
    public void handleOrderEvent(Object event) {
        if (event instanceof OrderPlacedEvent) {
            handleOrderPlaced((OrderPlacedEvent) event);
        } else if (event instanceof OrderCompletedEvent) {
            handleOrderCompleted((OrderCompletedEvent) event);
        } else if (event instanceof OrderCancelledEvent) {
            handleOrderCancelled((OrderCancelledEvent) event);
        } else {
            log.warn("Received unknown event type: {}", event.getClass().getName());
        }
    }

    private void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed for order {}", event.orderId());
        for (OrderItemPayload item : event.items()) {
            int updatedRows = productRepository.deductStockIfAvailable(item.productId(), item.quantity());
            if (updatedRows == 0) {
                log.warn("Failed to deduct stock for product {}. Product might be out of stock or already processed.", item.productId());
            } else {
                cacheInvalidationService.invalidateProductCaches(item.productId());
            }
        }
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received order.completed for order {}", event.orderId());
        cacheInvalidationService.invalidateTopRatedAndDashboardCaches();
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled for order {}", event.orderId());
        for (OrderItemPayload item : event.restoredItems()) {
            productRepository.restoreStock(item.productId(), item.quantity());
            cacheInvalidationService.invalidateProductCaches(item.productId());
        }
        cacheInvalidationService.invalidateTopRatedAndDashboardCaches();
    }
}
