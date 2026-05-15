package com.team14.amazon.shipping.service;

import com.team14.amazon.contracts.events.ShipmentCancelledEvent;
import com.team14.amazon.contracts.events.ShipmentCreatedEvent;
import com.team14.amazon.contracts.events.ShipmentStatusChangedEvent;
import com.team14.amazon.shipping.config.RabbitMQConfig;
import com.team14.amazon.shipping.model.Shipment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ShipmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishShipmentCreated(Shipment s) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SHIPMENT_EXCHANGE, "shipment.created",
                new ShipmentCreatedEvent(s.getId(), s.getOrderId(), s.getCarrier(), s.getTrackingNumber())
        );
    }

    public void publishShipmentStatusChanged(Shipment s) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SHIPMENT_EXCHANGE, "shipment.status-changed",
                new ShipmentStatusChangedEvent(s.getId(), s.getOrderId(), s.getStatus().name())
        );
    }

    public void publishShipmentCancelled(Shipment s) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SHIPMENT_EXCHANGE, "shipment.cancelled",
                new ShipmentCancelledEvent(s.getId(), s.getOrderId())
        );
    }
}