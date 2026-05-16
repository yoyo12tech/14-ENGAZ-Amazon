package com.team14.amazon.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.events.OrderItemPayload;
import com.team14.amazon.contracts.events.OrderPlacedEvent;
import com.team14.amazon.product.config.RabbitMQConfig;
import com.team14.amazon.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProductRepository productRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public OrderEventListener(ProductRepository productRepository,
                              CacheInvalidationService cacheInvalidationService) {
        this.productRepository = productRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_ORDER_QUEUE)
    @Transactional
    public void onOrderEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String corrId = message.getMessageProperties().getHeader("X-Correlation-ID");
        if (corrId != null) MDC.put("correlationId", corrId);
        if (routingKey != null) MDC.put("routingKey", routingKey);

        try {
            switch (routingKey != null ? routingKey : "") {
                case "order.placed"    -> handleOrderPlaced(message);
                case "order.completed" -> handleOrderCompleted(message);
                case "order.cancelled" -> handleOrderCancelled(message);
                default -> log.warn("Unhandled routing key on product queue: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("routingKey");
        }
    }

    // Idempotent: WHERE stock_quantity >= qty prevents double-deduction
    private void handleOrderPlaced(Message message) throws Exception {
        OrderPlacedEvent event = MAPPER.readValue(message.getBody(), OrderPlacedEvent.class);
        log.info("Consuming order.placed for orderId={}", event.orderId());
        for (OrderItemPayload item : event.items()) {
            int updated = productRepository.deductStockIfAvailable(item.productId(), item.quantity());
            if (updated == 0) {
                log.warn("Stock deduction skipped for productId={} (already processed or insufficient stock)",
                        item.productId());
            } else {
                log.info("Deducted {} units from productId={}", item.quantity(), item.productId());
                cacheInvalidationService.invalidateProductCaches(item.productId());
            }
        }
        log.info("Processed order.placed for orderId={}", event.orderId());
    }

    private void handleOrderCompleted(Message message) throws Exception {
        OrderCompletedEvent event = MAPPER.readValue(message.getBody(), OrderCompletedEvent.class);
        log.info("Consuming order.completed for orderId={}", event.orderId());
        cacheInvalidationService.invalidateTopRatedAndDashboardCaches();
        log.info("Processed order.completed for orderId={}", event.orderId());
    }

    private void handleOrderCancelled(Message message) throws Exception {
        OrderCancelledEvent event = MAPPER.readValue(message.getBody(), OrderCancelledEvent.class);
        log.info("Consuming order.cancelled for orderId={}", event.orderId());
        if (event.restoredItems() != null) {
            for (OrderItemPayload item : event.restoredItems()) {
                productRepository.restoreStock(item.productId(), item.quantity());
                log.info("Restored {} units to productId={}", item.quantity(), item.productId());
                cacheInvalidationService.invalidateProductCaches(item.productId());
            }
        }
        cacheInvalidationService.invalidateTopRatedAndDashboardCaches();
        log.info("Processed order.cancelled for orderId={}", event.orderId());
    }
}
