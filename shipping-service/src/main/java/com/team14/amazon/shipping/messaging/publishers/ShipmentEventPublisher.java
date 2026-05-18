package com.team14.amazon.shipping.messaging.publishers;

import com.team14.amazon.contracts.events.ShipmentCancelledEvent;
import com.team14.amazon.contracts.events.ShipmentCreatedEvent;
import com.team14.amazon.contracts.events.ShipmentStatusChangedEvent;
import com.team14.amazon.shipping.model.Shipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ShipmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishShipmentCreated(ShipmentCreatedEvent event) {
        System.out.println("He did publish ittttttttttttttttttttttttttttttttttttttttttttttttttttttttttt");

        log.info("Publishing shipment.created: shipmentId={} orderId={}", event.shipmentId(), event.orderId());
        rabbitTemplate.convertAndSend("shipment.events", "shipment.created", event);
    }

    public void publishShipmentStatusChanged(ShipmentStatusChangedEvent event) {
        log.info("Publishing shipment.status-changed: shipmentId={} orderId={} newStatus={}",
                event.shipmentId(), event.orderId(), event.newStatus());
        rabbitTemplate.convertAndSend("shipment.events", "shipment.status-changed", event);
    }

    public void publishShipmentCancelled(ShipmentCancelledEvent event) {
        log.info("Publishing shipment.cancelled: shipmentId={} orderId={}",
                event.shipmentId(), event.orderId());
        rabbitTemplate.convertAndSend("shipment.events", "shipment.cancelled", event);
    }
}