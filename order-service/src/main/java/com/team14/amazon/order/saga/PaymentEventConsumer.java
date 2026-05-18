//package com.team14.amazon.order.saga;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.team14.amazon.contracts.events.*;
//import com.team14.amazon.order.config.OrderEventConfig;
//import com.team14.amazon.order.messaging.OrderEventPublisher;
//import com.team14.amazon.order.model.Order;
//import com.team14.amazon.order.model.OrderStatus;
//import com.team14.amazon.order.repository.OrderRepository;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * Consumes all 4 payment saga events from order.saga-feedback queue.
// *
// * FIX (Bug 1): Replaced 4 separate @RabbitListener methods (which competed
// * for the same queue and caused only one to ever execute) with a SINGLE
// * @RabbitListener that receives the raw Message and dispatches via switch
// * on the routingKey — exactly the same pattern used by ShipmentEventConsumer
// * and product-service's OrderEventListener.
// *
// * Root cause of the original bug:
// *   Spring AMQP creates ONE SimpleMessageListenerContainer per queue name.
// *   Multiple @RabbitListener methods on the same queue do NOT create multiple
// *   containers — they register multiple handlers, but only one wins per message.
// *   The routing-key guard (if !routingKey.equals(...) return;) means the wrong
// *   handler returns early and drops the message without processing it.
// */
//@Component
//public class PaymentEventConsumer {
//
//    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
//    private static final ObjectMapper MAPPER = new ObjectMapper();
//
//    private final OrderRepository     orderRepository;
//    private final OrderEventPublisher eventPublisher;
//
//    public PaymentEventConsumer(OrderRepository orderRepository,
//                                OrderEventPublisher eventPublisher) {
//        this.orderRepository = orderRepository;
//        this.eventPublisher  = eventPublisher;
//    }
//
//    @PostConstruct
//    public void init() {
//        System.out.println("🔥 PaymentEventConsumer LOADED");
//    }
//
//    // =========================================================================
//    // SINGLE entry point — all payment events arrive here
//    // =========================================================================
//
//    @RabbitListener(queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE)
//    @Transactional
//    public void onPaymentEvent(Message message) {
//
//
//        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
//        String corrId     = message.getMessageProperties().getHeader("X-Correlation-ID");
//
//        if (corrId     != null) MDC.put("correlationId", corrId);
//        if (routingKey != null) MDC.put("routingKey",    routingKey);
//        System.out.println("He tried to consume something in terms of PaymentEvents");
//        System.out.println("The routingKey is " + routingKey);
//        try {
//            switch (routingKey != null ? routingKey : "") {
//
//                case OrderEventConfig.ROUTING_PAYMENT_INITIATED ->
//                        handlePaymentInitiated(message);
//
//                case OrderEventConfig.ROUTING_PAYMENT_COMPLETED ->
//                        handlePaymentCompleted(message);
//
//                case OrderEventConfig.ROUTING_PAYMENT_FAILED ->
//                        handlePaymentFailed(message);
//
//                case OrderEventConfig.ROUTING_PAYMENT_REFUNDED ->
//                        handlePaymentRefunded(message);
//
//                // Shipment events also arrive on this queue — ShipmentEventConsumer
//                // handles them. We silently skip them here.
//                case OrderEventConfig.ROUTING_SHIPMENT_CREATED,
//                     OrderEventConfig.ROUTING_SHIPMENT_STATUS_CHANGED ->
//                        log.debug("Skipping shipment event in PaymentEventConsumer: {}", routingKey);
//
//                default ->
//                        log.warn("Unhandled routing key on payment saga queue: {}", routingKey);
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to process {}: {}", routingKey, e.getMessage(), e);
//            throw new RuntimeException(e); // rethrow → Spring retries → DLQ after max-attempts
//        } finally {
//            MDC.clear();
//        }
//    }
//
//    // =========================================================================
//    // payment.initiated → DELIVERED → PAYMENT_PENDING
//    // =========================================================================
//
//    private void handlePaymentInitiated(Message message) throws Exception {
//        PaymentInitiatedEvent event = MAPPER.readValue(message.getBody(), PaymentInitiatedEvent.class);
//
//        log.info("Consuming payment.initiated for orderId={} transactionId={}",
//                event.orderId(), event.transactionId());
//
//        int updated = orderRepository.updateOrderStatus(
//                event.orderId(),
//                OrderStatus.PAYMENT_PENDING,
//                OrderStatus.DELIVERED);
//
//        if (updated == 0) {
//            log.warn("payment.initiated: orderId={} not in DELIVERED state. Skipping.",
//                    event.orderId());
//            return;
//        }
//
//        log.info("Order {} transitioning DELIVERED → PAYMENT_PENDING", event.orderId());
//    }
//
//    // =========================================================================
//    // payment.completed → PAYMENT_PENDING → PAID
//    // =========================================================================
//
//    private void handlePaymentCompleted(Message message) throws Exception {
//        PaymentCompletedEvent event = MAPPER.readValue(message.getBody(), PaymentCompletedEvent.class);
//
//        log.info("Consuming payment.completed for orderId={} transactionId={}",
//                event.orderId(), event.transactionId());
//
//        int updated = orderRepository.updateOrderStatus(
//                event.orderId(),
//                OrderStatus.PAID,
//                OrderStatus.PAYMENT_PENDING);
//
//        if (updated == 0) {
//            log.warn("payment.completed: orderId={} not in PAYMENT_PENDING state. Skipping.",
//                    event.orderId());
//            return;
//        }
//
//        log.info("Order {} transitioning PAYMENT_PENDING → PAID", event.orderId());
//    }
//
//    // =========================================================================
//    // payment.failed → PAYMENT_PENDING → PAYMENT_FAILED + compensation
//    // =========================================================================
//
//    private void handlePaymentFailed(Message message) throws Exception {
//        PaymentFailedEvent event = MAPPER.readValue(message.getBody(), PaymentFailedEvent.class);
//
//        log.info("Consuming payment.failed for orderId={} reason={}",
//                event.orderId(), event.reason());
//
//        int updated = orderRepository.updateOrderStatus(
//                event.orderId(),
//                OrderStatus.PAYMENT_FAILED,
//                OrderStatus.PAYMENT_PENDING);
//
//        if (updated == 0) {
//            log.warn("payment.failed: orderId={} not in PAYMENT_PENDING state. Skipping compensation.",
//                    event.orderId());
//            return;
//        }
//
//        log.info("Order {} transitioning PAYMENT_PENDING → PAYMENT_FAILED", event.orderId());
//
//        // Compensation cascade: publish order.cancelled so product/shipping/billing
//        // can reverse their local state.
//        Order order = orderRepository.findById(event.orderId()).orElse(null);
//        if (order == null) {
//            log.error("Compensation: orderId={} not found after status update. Cannot publish order.cancelled.",
//                    event.orderId());
//            return;
//        }
//
//        List<OrderItemPayload> restoredItems = order.getOrderItems().stream()
//                .map(item -> new OrderItemPayload(
//                        item.getProductId(),
//                        item.getQuantity(),
//                        item.getPriceAtPurchase()))
//                .collect(Collectors.toList());
//
//        eventPublisher.publishOrderCancelled(new OrderCancelledEvent(
//                event.orderId(),
//                order.getUserId(),
//                restoredItems,
//                "payment_failed"));
//
//        log.info("Published order.cancelled (compensation) for orderId={}", event.orderId());
//    }
//
//    // =========================================================================
//    // payment.refunded → PAYMENT_FAILED → REFUNDED
//    // =========================================================================
//
//    private void handlePaymentRefunded(Message message) throws Exception {
//        PaymentRefundedEvent event = MAPPER.readValue(message.getBody(), PaymentRefundedEvent.class);
//
//        log.info("Consuming payment.refunded for orderId={} refundAmount={}",
//                event.orderId(), event.refundAmount());
//
//        int updated = orderRepository.updateOrderStatus(
//                event.orderId(),
//                OrderStatus.REFUNDED,
//                OrderStatus.PAYMENT_FAILED);
//
//        if (updated == 0) {
//            log.warn("payment.refunded: orderId={} not in PAYMENT_FAILED state. Skipping.",
//                    event.orderId());
//            return;
//        }
//
//        log.info("Order {} transitioning PAYMENT_FAILED → REFUNDED", event.orderId());
//    }
//}