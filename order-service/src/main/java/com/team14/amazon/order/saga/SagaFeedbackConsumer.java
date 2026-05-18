package com.team14.amazon.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team14.amazon.contracts.events.*;
import com.team14.amazon.order.config.OrderEventConfig;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.messaging.publishers.OrderEventPublisher;
import com.team14.amazon.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Single @RabbitListener on order.saga-feedback — dispatches by routing key.
 *
 * Replaces the old PaymentEventConsumer + ShipmentEventConsumer which each
 * registered a separate AMQP consumer on the same queue. With multiple
 * consumers on one queue, AMQP delivers each message to exactly one of them —
 * so a payment.completed message consumed by the shipmentCreated listener
 * would be silently ACKed and dropped.
 *
 * One listener = one AMQP consumer = every message processed by the correct handler.
 */
@Component
public class SagaFeedbackConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaFeedbackConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final OrderRepository     orderRepository;
    private final OrderEventPublisher eventPublisher;

    public SagaFeedbackConsumer(OrderRepository orderRepository,
                                OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher  = eventPublisher;
    }

    @RabbitListener(queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE)
    @Transactional
    public void onSagaFeedback(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String corrId     = message.getMessageProperties().getHeader("X-Correlation-ID");
        if (corrId != null)     MDC.put("correlationId", corrId);
        if (routingKey != null) MDC.put("routingKey", routingKey);

        try {
            switch (routingKey != null ? routingKey : "") {
                case OrderEventConfig.ROUTING_PAYMENT_INITIATED       -> handlePaymentInitiated(message);
                case OrderEventConfig.ROUTING_PAYMENT_COMPLETED       -> handlePaymentCompleted(message);
                case OrderEventConfig.ROUTING_PAYMENT_FAILED          -> handlePaymentFailed(message);
                case OrderEventConfig.ROUTING_PAYMENT_REFUNDED        -> handlePaymentRefunded(message);
                case OrderEventConfig.ROUTING_SHIPMENT_CREATED        -> handleShipmentCreated(message);
                case OrderEventConfig.ROUTING_SHIPMENT_STATUS_CHANGED -> handleShipmentStatusChanged(message);
                default -> log.warn("Unhandled routing key on saga-feedback queue: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("routingKey");
            MDC.remove("orderId");
            MDC.remove("transactionId");
            MDC.remove("shipmentId");
        }
    }

    // ── payment.initiated → DELIVERED → PAYMENT_PENDING ──────────────────────

    private void handlePaymentInitiated(Message message) throws Exception {
        PaymentInitiatedEvent event = MAPPER.readValue(message.getBody(), PaymentInitiatedEvent.class);
        MDC.put("orderId", String.valueOf(event.orderId()));
        MDC.put("transactionId", String.valueOf(event.transactionId()));
        log.info("Consuming payment.initiated for orderId={} transactionId={}", event.orderId(), event.transactionId());

        int updated = orderRepository.updateOrderStatus(
                event.orderId(), OrderStatus.PAYMENT_PENDING, OrderStatus.DELIVERED);

        if (updated == 0) {
            log.warn("payment.initiated for orderId={} — not in DELIVERED state. Skipping.", event.orderId());
            return;
        }
        log.info("Order {} transitioning DELIVERED → PAYMENT_PENDING", event.orderId());
        log.info("Processed payment.initiated for orderId={}", event.orderId());
    }

    // ── payment.completed → PAYMENT_PENDING → PAID ───────────────────────────

    private void handlePaymentCompleted(Message message) throws Exception {
        PaymentCompletedEvent event = MAPPER.readValue(message.getBody(), PaymentCompletedEvent.class);
        MDC.put("orderId", String.valueOf(event.orderId()));
        MDC.put("transactionId", String.valueOf(event.transactionId()));
        log.info("Consuming payment.completed for orderId={} transactionId={}", event.orderId(), event.transactionId());

        // Try PAYMENT_PENDING → PAID (normal path)
        int updated = orderRepository.updateOrderStatus(
                event.orderId(), OrderStatus.PAID, OrderStatus.PAYMENT_PENDING);

        if (updated == 0) {
            // Race condition: payment processed before payment.initiated was consumed by order-service.
            // Order is still DELIVERED — transition directly to PAID.
            updated = orderRepository.updateOrderStatus(
                    event.orderId(), OrderStatus.PAID, OrderStatus.DELIVERED);
        }

        if (updated == 0) {
            log.warn("payment.completed for orderId={} — not in PAYMENT_PENDING or DELIVERED state. Skipping.", event.orderId());
            return;
        }
        log.info("Order {} transitioning → PAID", event.orderId());
        log.info("Processed payment.completed for orderId={}", event.orderId());
    }

    // ── payment.failed → PAYMENT_PENDING → PAYMENT_FAILED + compensation ─────

    private void handlePaymentFailed(Message message) throws Exception {
        PaymentFailedEvent event = MAPPER.readValue(message.getBody(), PaymentFailedEvent.class);
        MDC.put("orderId", String.valueOf(event.orderId()));
        log.info("Consuming payment.failed for orderId={} reason={}", event.orderId(), event.reason());

        int updated = orderRepository.updateOrderStatus(
                event.orderId(), OrderStatus.PAYMENT_FAILED, OrderStatus.PAYMENT_PENDING);

        if (updated == 0) {
            log.warn("payment.failed for orderId={} — not in PAYMENT_PENDING state. Skipping compensation.", event.orderId());
            return;
        }
        log.info("Order {} transitioning PAYMENT_PENDING → PAYMENT_FAILED", event.orderId());

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.error("Compensation failed for orderId={}: order not found after status update", event.orderId());
            return;
        }

        List<OrderItemPayload> restoredItems = order.getOrderItems().stream()
                .map(item -> new OrderItemPayload(
                        item.getProductId(), item.getQuantity(), item.getPriceAtPurchase()))
                .collect(Collectors.toList());

        eventPublisher.publishOrderCancelled(new OrderCancelledEvent(
                event.orderId(), order.getUserId(), restoredItems, "payment_failed"));

        log.info("Published order.cancelled (compensation) for orderId={}", event.orderId());
        log.info("Processed payment.failed for orderId={}", event.orderId());
    }

    // ── payment.refunded → PAYMENT_FAILED → REFUNDED ─────────────────────────

    private void handlePaymentRefunded(Message message) throws Exception {
        PaymentRefundedEvent event = MAPPER.readValue(message.getBody(), PaymentRefundedEvent.class);
        MDC.put("orderId", String.valueOf(event.orderId()));
        log.info("Consuming payment.refunded for orderId={} refundAmount={}", event.orderId(), event.refundAmount());

        int updated = orderRepository.updateOrderStatus(
                event.orderId(), OrderStatus.REFUNDED, OrderStatus.PAYMENT_FAILED);

        if (updated == 0) {
            log.warn("payment.refunded for orderId={} — not in PAYMENT_FAILED state. Skipping.", event.orderId());
            return;
        }
        log.info("Order {} transitioning PAYMENT_FAILED → REFUNDED", event.orderId());
        log.info("Processed payment.refunded for orderId={}", event.orderId());
    }

    // ── shipment.created → store shipmentId on Order ──────────────────────────

    private void handleShipmentCreated(Message message) throws Exception {
        ShipmentCreatedEvent event = MAPPER.readValue(message.getBody(), ShipmentCreatedEvent.class);
        MDC.put("orderId", String.valueOf(event.orderId()));
        MDC.put("shipmentId", String.valueOf(event.shipmentId()));
        log.info("Consuming shipment.created for orderId={} shipmentId={}", event.orderId(), event.shipmentId());

        int updated = orderRepository.updateShipmentId(event.orderId(), event.shipmentId());

        if (updated == 0) {
            log.warn("shipment.created for orderId={} — shipmentId already set. Skipping.", event.orderId());
            return;
        }
        log.info("Processed shipment.created for orderId={} shipmentId={}", event.orderId(), event.shipmentId());
    }

    // ── shipment.status-changed → mirror status in order metadata ─────────────

    private void handleShipmentStatusChanged(Message message) throws Exception {
        ShipmentStatusChangedEvent event = MAPPER.readValue(message.getBody(), ShipmentStatusChangedEvent.class);
        MDC.put("orderId", String.valueOf(event.orderId()));
        MDC.put("shipmentId", String.valueOf(event.shipmentId()));
        log.info("Consuming shipment.status-changed for orderId={} newStatus={}", event.orderId(), event.newStatus());

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("shipment.status-changed for orderId={} — order not found. Skipping.", event.orderId());
            return;
        }
        order.getMetadata().put("latestShipmentStatus", event.newStatus());
        order.getMetadata().put("latestShipmentId", event.shipmentId());
        orderRepository.save(order);

        log.info("Processed shipment.status-changed for orderId={} shipmentStatus={}", event.orderId(), event.newStatus());
    }
}