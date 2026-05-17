package com.team14.amazon.shipping.messaging.consumers;

import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.events.OrderPlacedEvent;
import com.team14.amazon.contracts.events.ShipmentCancelledEvent;
import com.team14.amazon.contracts.events.ShipmentStatusChangedEvent;
import com.team14.amazon.shipping.messaging.publishers.ShipmentEventPublisher;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final Set<Shipment.Status> ACTIVE_STATUSES = EnumSet.of(
            Shipment.Status.PROCESSING,
            Shipment.Status.SHIPPED,
            Shipment.Status.IN_TRANSIT,
            Shipment.Status.OUT_FOR_DELIVERY
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventPublisher shipmentEventPublisher;

    public OrderEventConsumer(ShipmentRepository shipmentRepository,
                              ShipmentEventPublisher shipmentEventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventPublisher = shipmentEventPublisher;
    }

    @RabbitListener(queues = "shipment.saga-listener")
    public void onOrderEvent(Object event) {
        if (event instanceof OrderPlacedEvent placedEvent) {
            handleOrderPlaced(placedEvent);
        } else if (event instanceof OrderCompletedEvent completedEvent) {
            handleOrderCompleted(completedEvent);
        } else if (event instanceof OrderCancelledEvent cancelledEvent) {
            handleOrderCancelled(cancelledEvent);
        }
    }

    private void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for order {}", event.orderId());
        // No shipment created here — warehouse staff call POST /api/shipments/order/{orderId}
        // (S4-F2) when the package is ready. This handler logs the incoming order
        // for delivery slot planning only.
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received OrderCompletedEvent for order {}", event.orderId());

        Optional<Shipment> activeShipmentOpt =
                shipmentRepository.findFirstByOrderIdAndStatusIn(event.orderId(), ACTIVE_STATUSES);

        if (activeShipmentOpt.isEmpty()) {
            // Idempotency: already DELIVERED on a previous delivery, or no shipment exists.
            log.warn("No active shipment found for orderId={} — already processed or missing. Skipping.",
                    event.orderId());
            return;
        }

        Shipment shipment = activeShipmentOpt.get();
        shipment.setStatus(Shipment.Status.DELIVERED);
        shipment.setActualDelivery(LocalDateTime.now());
        shipment.setLastUpdate(LocalDateTime.now());
        shipmentRepository.save(shipment);

        log.info("Shipment {} marked DELIVERED for orderId={}", shipment.getId(), event.orderId());

        shipmentEventPublisher.publishShipmentStatusChanged(
                new ShipmentStatusChangedEvent(shipment.getId(), event.orderId(), "DELIVERED"));
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order {}", event.orderId());

        Optional<Shipment> activeShipmentOpt =
                shipmentRepository.findFirstByOrderIdAndStatusIn(event.orderId(), ACTIVE_STATUSES);

        if (activeShipmentOpt.isEmpty()) {
            // Idempotency: already CANCELLED, or order was PENDING with no shipment yet.
            log.warn("No active shipment found for orderId={} — already cancelled or never shipped. Skipping.",
                    event.orderId());
            return;
        }

        Shipment shipment = activeShipmentOpt.get();
        shipment.setStatus(Shipment.Status.CANCELLED);
        shipment.setLastUpdate(LocalDateTime.now());
        shipmentRepository.save(shipment);

        log.info("Shipment {} CANCELLED for orderId={}", shipment.getId(), event.orderId());

        shipmentEventPublisher.publishShipmentCancelled(
                new ShipmentCancelledEvent(shipment.getId(), event.orderId()));
    }
}