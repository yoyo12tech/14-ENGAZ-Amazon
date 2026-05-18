package com.team14.amazon.order.saga;

import com.team14.amazon.contracts.events.ShipmentCreatedEvent;
import com.team14.amazon.contracts.events.ShipmentStatusChangedEvent;
import com.team14.amazon.order.config.OrderEventConfig;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes shipment events from order.saga-feedback queue.
 *
 * Lives in saga/ package per spec §12 folder tree.
 *
 * Spec §5 RabbitMQ S3 Consumes:
 *   shipment.created       → store shipmentId on the Order
 *   shipment.status-changed → mirror shipment status in order metadata
 *
 * Idempotency (Critical Rule #5):
 *   shipment.created     → updateShipmentId() uses WHERE shipment_id IS NULL
 *                          so a duplicate event is a no-op (rowcount == 0).
 *   shipment.status-changed → metadata update is last-write-wins; replaying
 *                             the same event sets the same value — harmless.
 *
 * Do NOT call basicAck/basicNack — Spring auto-ACK handles it.
 * On exception after max-attempts=3 → message routes to DLQ automatically.
 */
@Component
public class ShipmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventConsumer.class);

    private final OrderRepository orderRepository;

    public ShipmentEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // =========================================================================
    // shipment.created → store shipmentId on the Order
    // =========================================================================

    @RabbitListener(
            queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE,
            id     = "shipmentCreatedListener"
    )
    @Transactional
    public void onShipmentCreated(
            ShipmentCreatedEvent event,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        // Route guard — this listener receives all messages on the queue;
        // only process the ones with the correct routing key.
        if (!OrderEventConfig.ROUTING_SHIPMENT_CREATED.equals(routingKey)) return;

        setupMdc(correlationId, event.orderId(), routingKey);
        try {
            log.info("Consuming {} for orderId={} shipmentId={}",
                    routingKey, event.orderId(), event.shipmentId());

            // Idempotent: updateShipmentId uses WHERE shipment_id IS NULL
            int updated = orderRepository.updateShipmentId(event.orderId(), event.shipmentId());

            if (updated == 0) {
                log.warn("Processed {} for orderId={} — shipmentId already set or order not found. Skipping.",
                        routingKey, event.orderId());
                return;
            }

            log.info("Processed {} for orderId={} shipmentId={}",
                    routingKey, event.orderId(), event.shipmentId());

        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage(), ex);
            throw ex; // rethrow → Spring retries → DLQ after max-attempts
        } finally {
            clearMdc();
        }
    }

    // =========================================================================
    // shipment.status-changed → mirror latest shipment status in order metadata
    // =========================================================================

    @RabbitListener(
            queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE,
            id     = "shipmentStatusChangedListener"
    )
    @Transactional
    public void onShipmentStatusChanged(
            ShipmentStatusChangedEvent event,
            @Header(value = "amqp_receivedRoutingKey", required = false) String routingKey,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        if (!OrderEventConfig.ROUTING_SHIPMENT_STATUS_CHANGED.equals(routingKey)) return;

        setupMdc(correlationId, event.orderId(), routingKey);
        try {
            log.info("Consuming {} for orderId={} newStatus={}",
                    routingKey, event.orderId(), event.newStatus());

            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                log.warn("Processed {} for orderId={} — order not found. Skipping.",
                        routingKey, event.orderId());
                return;
            }

            // Mirror shipment status into metadata for fast S3-F1 / dashboard queries
            order.getMetadata().put("latestShipmentStatus", event.newStatus());
            order.getMetadata().put("latestShipmentId",     event.shipmentId());
            orderRepository.save(order);

            log.info("Processed {} for orderId={} shipmentStatus={}",
                    routingKey, event.orderId(), event.newStatus());

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
        MDC.remove("shipmentId");
    }
}