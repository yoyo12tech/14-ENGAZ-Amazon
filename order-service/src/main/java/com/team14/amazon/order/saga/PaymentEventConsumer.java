package com.team14.amazon.order.saga;

import com.team14.amazon.contracts.events.*;
import com.team14.amazon.order.config.OrderEventConfig;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.messaging.OrderEventPublisher;
import com.team14.amazon.order.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumes all 4 payment saga events from order.saga-feedback queue.
 *
 * Lives in saga/ package per spec §12 folder tree.
 *
 * Spec §5 RabbitMQ S3 Consumes:
 *   payment.initiated  → DELIVERED      → PAYMENT_PENDING
 *   payment.completed  → PAYMENT_PENDING → PAID
 *   payment.failed     → PAYMENT_PENDING → PAYMENT_FAILED + publish order.cancelled (compensation)
 *   payment.refunded   → PAYMENT_FAILED  → REFUNDED
 *
 * Idempotency (Critical Rule #5 — Approach 2):
 *   Every status transition uses updateOrderStatus(id, newStatus, expectedStatus).
 *   rowcount == 0 means the order is already in a different state (duplicate or
 *   out-of-order delivery) — log and skip, never throw.
 *
 * Compensation (payment.failed):
 *   When payment fails, order-service is responsible for publishing order.cancelled
 *   so that product-service restores stock, shipping-service cancels the shipment,
 *   and billing-service issues a refund.  This happens only when the atomic status
 *   transition succeeds (rowcount == 1) to avoid duplicate compensation.
 *
 * Do NOT call basicAck/basicNack. Spring auto-ACK + no-requeue + DLQ handles failures.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderRepository     orderRepository;
    private final OrderEventPublisher eventPublisher;

    public PaymentEventConsumer(OrderRepository orderRepository,
                                OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher  = eventPublisher;
    }

    // =========================================================================
    // payment.initiated → DELIVERED → PAYMENT_PENDING
    // =========================================================================
    @PostConstruct
    public void init() {
        System.out.println("🔥 PaymentEventConsumer LOADED");
    }

    @RabbitListener(
            queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE,
            id     = "paymentInitiatedListener"
    )
    @Transactional
    public void onPaymentInitiated(
            PaymentInitiatedEvent event,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        if (!OrderEventConfig.ROUTING_PAYMENT_INITIATED.equals(routingKey)) return;

        setupMdc(correlationId, event.orderId(), routingKey);
        try {
            log.info("Consuming {} for orderId={} transactionId={}",
                    routingKey, event.orderId(), event.transactionId());

            // Atomic idempotent transition: DELIVERED → PAYMENT_PENDING
            int updated = orderRepository.updateOrderStatus(
                    event.orderId(),
                    OrderStatus.PAYMENT_PENDING, //law dah gably error lazm a3mlha beyh .name() y3ni a7welha leyh string
                    OrderStatus.DELIVERED);

            if (updated == 0) {
                log.warn("Processed {} for orderId={} — not in DELIVERED state or already processed. Skipping.",
                        routingKey, event.orderId());
                return;
            }

            log.info("Order {} transitioning DELIVERED → PAYMENT_PENDING", event.orderId());
            log.info("Processed {} for orderId={}", routingKey, event.orderId());

        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    // =========================================================================
    // payment.completed → PAYMENT_PENDING → PAID
    // =========================================================================

    @RabbitListener(
            queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE,
            id     = "paymentCompletedListener"
    )
    @Transactional
    public void onPaymentCompleted(
            PaymentCompletedEvent event,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        if (!OrderEventConfig.ROUTING_PAYMENT_COMPLETED.equals(routingKey)) return;

        setupMdc(correlationId, event.orderId(), routingKey);
        try {
            log.info("Consuming {} for orderId={} transactionId={}",
                    routingKey, event.orderId(), event.transactionId());

            // Atomic idempotent transition: PAYMENT_PENDING → PAID
            int updated = orderRepository.updateOrderStatus(
                    event.orderId(),
                    OrderStatus.PAID,
                    OrderStatus.PAYMENT_PENDING);

            if (updated == 0) {
                log.warn("Processed {} for orderId={} — not in PAYMENT_PENDING state. Skipping.",
                        routingKey, event.orderId());
                return;
            }

            log.info("Order {} transitioning PAYMENT_PENDING → PAID", event.orderId());
            log.info("Processed {} for orderId={}", routingKey, event.orderId());

        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    // =========================================================================
    // payment.failed → PAYMENT_PENDING → PAYMENT_FAILED + compensation cascade
    // =========================================================================

    @RabbitListener(
            queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE,
            id     = "paymentFailedListener"
    )
    @Transactional
    public void onPaymentFailed(
            PaymentFailedEvent event,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        if (!OrderEventConfig.ROUTING_PAYMENT_FAILED.equals(routingKey)) return;

        setupMdc(correlationId, event.orderId(), routingKey);
        try {
            log.info("Consuming {} for orderId={} reason={}",
                    routingKey, event.orderId(), event.reason());

            // Atomic idempotent transition: PAYMENT_PENDING → PAYMENT_FAILED
            int updated = orderRepository.updateOrderStatus(
                    event.orderId(),
                    OrderStatus.PAYMENT_FAILED,
                    OrderStatus.PAYMENT_PENDING);

            if (updated == 0) {
                log.warn("Processed {} for orderId={} — not in PAYMENT_PENDING state. Skipping compensation.",
                        routingKey, event.orderId());
                return;
            }

            log.info("Order {} transitioning PAYMENT_PENDING → PAYMENT_FAILED", event.orderId());

            // ── COMPENSATION CASCADE ───────────────────────────────────────────
            // Publish order.cancelled so:
            //   product-service  → restores stock (was deducted by order.placed)
            //   shipping-service → cancels the shipment
            //   billing-service  → refunds any completed payment (issues payment.refunded)
            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                log.error("Failed to process compensation for orderId={}: order not found after status update",
                        event.orderId());
                return;
            }

            List<OrderItemPayload> restoredItems = order.getOrderItems().stream()
                    .map(item -> new OrderItemPayload(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getPriceAtPurchase()))
                    .collect(Collectors.toList());

            OrderCancelledEvent cancelEvent = new OrderCancelledEvent(
                    event.orderId(),
                    order.getUserId(),
                    restoredItems,
                    "payment_failed");

            eventPublisher.publishOrderCancelled(cancelEvent);
            log.info("Published order.cancelled (compensation) for orderId={}", event.orderId());
            log.info("Processed {} for orderId={}", routingKey, event.orderId());

        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    // =========================================================================
    // payment.refunded → PAYMENT_FAILED → REFUNDED
    // =========================================================================

    @RabbitListener(
            queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE,
            id     = "paymentRefundedListener"
    )
    @Transactional
    public void onPaymentRefunded(
            PaymentRefundedEvent event,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        if (!OrderEventConfig.ROUTING_PAYMENT_REFUNDED.equals(routingKey)) return;

        setupMdc(correlationId, event.orderId(), routingKey);
        try {
            log.info("Consuming {} for orderId={} refundAmount={}",
                    routingKey, event.orderId(), event.refundAmount());

            // Atomic idempotent transition: PAYMENT_FAILED → REFUNDED
            int updated = orderRepository.updateOrderStatus(
                    event.orderId(),
                    OrderStatus.REFUNDED,
                    OrderStatus.PAYMENT_FAILED);

            if (updated == 0) {
                log.warn("Processed {} for orderId={} — not in PAYMENT_FAILED state. Skipping.",
                        routingKey, event.orderId());
                return;
            }

            log.info("Order {} transitioning PAYMENT_FAILED → REFUNDED", event.orderId());
            log.info("Processed {} for orderId={}", routingKey, event.orderId());

        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    // ── MDC helpers ───────────────────────────────────────────────────────────

    private void setupMdc(String correlationId, Long orderId, String routingKey) {
        if (correlationId != null) MDC.put("correlationId", correlationId);
        if (orderId != null)       MDC.put("orderId", orderId.toString());
        if (routingKey != null)    MDC.put("routingKey", routingKey);
    }

    private void clearMdc() {
        MDC.remove("correlationId");
        MDC.remove("orderId");
        MDC.remove("routingKey");
        MDC.remove("transactionId");
    }
}