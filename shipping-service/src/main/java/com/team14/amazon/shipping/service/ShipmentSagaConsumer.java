//package com.team14.amazon.shipping.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.team14.amazon.contracts.events.OrderCancelledEvent;
//import com.team14.amazon.contracts.events.OrderCompletedEvent;
//import com.team14.amazon.shipping.config.RabbitMQConfig;
//import com.team14.amazon.shipping.model.Shipment;
//import com.team14.amazon.shipping.repository.ShipmentRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.messaging.handler.annotation.Headers;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.MessageHeaders;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//public class ShipmentSagaConsumer {
//
//    private static final Logger log = LoggerFactory.getLogger(ShipmentSagaConsumer.class);
//
//    private final ShipmentRepository shipmentRepository;
//    private final ShipmentEventPublisher publisher;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public ShipmentSagaConsumer(ShipmentRepository shipmentRepository, ShipmentEventPublisher publisher) {
//        this.shipmentRepository = shipmentRepository;
//        this.publisher = publisher;
//    }
//
//    @RabbitListener(queues = RabbitMQConfig.SAGA_QUEUE)
//    public void handleOrderEvent(org.springframework.amqp.core.Message message) {
//        String rawJson = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
//        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
//        if (routingKey == null) return;
//
//        switch (routingKey) {
//            case "order.placed"    -> log.info("order.placed received — delivery slot noted: {}", rawJson);
//            case "order.completed" -> handleOrderCompleted(rawJson);
//            case "order.cancelled" -> handleOrderCancelled(rawJson);
//            default                -> log.warn("Unhandled routing key: {}", routingKey);
//        }
//    }
//
//    private void handleOrderCompleted(String json) {
//        try {
//            OrderCompletedEvent event = objectMapper.readValue(json, OrderCompletedEvent.class);
//            shipmentRepository.findActiveShipmentByOrderId(event.orderId()).ifPresentOrElse(
//                    shipment -> {
//                        shipment.setStatus(Shipment.Status.DELIVERED);
//                        shipment.setActualDelivery(LocalDateTime.now());
//                        shipment.setLastUpdate(LocalDateTime.now());
//                        shipmentRepository.save(shipment);
//                        publisher.publishShipmentStatusChanged(shipment);
//                        log.info("Shipment {} marked DELIVERED for order {}", shipment.getId(), event.orderId());
//                    },
//                    () -> log.warn("No active shipment found for completed order {}", event.orderId())
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to handle order.completed: " + e.getMessage(), e);
//        }
//    }
//
//    private void handleOrderCancelled(String json) {
//        try {
//            OrderCancelledEvent event = objectMapper.readValue(json, OrderCancelledEvent.class);
//            shipmentRepository.findActiveShipmentByOrderId(event.orderId()).ifPresentOrElse(
//                    shipment -> {
//                        if (shipment.getStatus() != Shipment.Status.DELIVERED) {
//                            shipment.setStatus(Shipment.Status.CANCELLED);
//                            shipment.setLastUpdate(LocalDateTime.now());
//                            shipmentRepository.save(shipment);
//                            publisher.publishShipmentCancelled(shipment);
//                            log.info("Shipment {} cancelled for order {}", shipment.getId(), event.orderId());
//                        }
//                    },
//                    () -> log.warn("No active shipment found for cancelled order {}", event.orderId())
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to handle order.cancelled: " + e.getMessage(), e);
//        }
//    }
//}