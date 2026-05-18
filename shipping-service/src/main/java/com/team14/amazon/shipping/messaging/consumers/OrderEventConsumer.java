package com.team14.amazon.shipping.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.events.ShipmentCancelledEvent;
import com.team14.amazon.contracts.events.ShipmentStatusChangedEvent;
import com.team14.amazon.shipping.messaging.publishers.ShipmentEventPublisher;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
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
            Shipment.Status.OUT_FOR_DELIVERY,
            Shipment.Status.DELIVERED
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventPublisher shipmentEventPublisher;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ShipmentRepository shipmentRepository,
                              ShipmentEventPublisher shipmentEventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventPublisher = shipmentEventPublisher;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @RabbitListener(queues = "shipment.saga-listener")
    public void onOrderEvent(Message message) {
        String json = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("Received message with routingKey={}", routingKey);

        try {
            switch (routingKey) {
                case "order.placed" -> log.info("order.placed received — delivery slot noted");
                case "order.completed" -> {
                    OrderCompletedEvent event = objectMapper.readValue(json, OrderCompletedEvent.class);
                    handleOrderCompleted(event);
                }
                case "order.cancelled" -> {
                    OrderCancelledEvent event = objectMapper.readValue(json, OrderCancelledEvent.class);
                    handleOrderCancelled(event);
                }
                default -> log.warn("Unhandled routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage());
            throw new RuntimeException("Failed to process " + routingKey + ": " + e.getMessage(), e);
        }
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received OrderCompletedEvent for orderId={}", event.orderId());

        Optional<Shipment> activeShipmentOpt =
                shipmentRepository.findFirstByOrderIdAndStatusIn(event.orderId(), ACTIVE_STATUSES);

        if (activeShipmentOpt.isEmpty()) {
            log.warn("No active shipment for orderId={} — skipping.", event.orderId());
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
        log.info("Received OrderCancelledEvent for orderId={}", event.orderId());

        Optional<Shipment> activeShipmentOpt =
                shipmentRepository.findFirstByOrderIdAndStatusIn(event.orderId(), ACTIVE_STATUSES);

        if (activeShipmentOpt.isEmpty()) {
            log.warn("No active shipment for orderId={} — skipping.", event.orderId());
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