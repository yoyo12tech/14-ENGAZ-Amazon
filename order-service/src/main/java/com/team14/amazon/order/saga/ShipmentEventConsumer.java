package com.team14.amazon.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team14.amazon.contracts.events.ShipmentCreatedEvent;
import com.team14.amazon.contracts.events.ShipmentStatusChangedEvent;
import com.team14.amazon.order.config.OrderEventConfig;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Component
public class ShipmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderRepository orderRepository;

    public ShipmentEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // =========================================================
    // Startup check
    // =========================================================

    @PostConstruct
    public void init() {
        System.out.println("🔥 ShipmentEventConsumer LOADED");
    }

    // =========================================================
    // SINGLE ENTRY POINT (like your product service style)
    // =========================================================

    @RabbitListener(queues = OrderEventConfig.SAGA_FEEDBACK_QUEUE)
    @Transactional
    public void onShipmentEvent(Message message) {

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String corrId = message.getMessageProperties().getHeader("X-Correlation-ID");

        if (corrId != null) MDC.put("correlationId", corrId);
        if (routingKey != null) MDC.put("routingKey", routingKey);

        try {
            switch (routingKey != null ? routingKey : "") {

                case OrderEventConfig.ROUTING_SHIPMENT_CREATED ->
                        handleShipmentCreated(message);

                case OrderEventConfig.ROUTING_SHIPMENT_STATUS_CHANGED ->
                        handleShipmentStatusChanged(message);

                default ->
                        log.warn("Unhandled routing key on shipment saga queue: {}", routingKey);
            }

        } catch (Exception e) {
            log.error("Failed to process {}: {}", routingKey, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            MDC.clear();
        }
    }

    // =========================================================
    // shipment.created
    // =========================================================

    private void handleShipmentCreated(Message message) throws Exception {

        ShipmentCreatedEvent event =
                MAPPER.readValue(message.getBody(), ShipmentCreatedEvent.class);

        log.info("Consuming shipment.created for orderId={} shipmentId={}",
                event.orderId(), event.shipmentId());
        System.out.println("This is what i recived as the event :"+event);


        int updated = orderRepository.updateShipmentId(
                event.orderId(),
                event.shipmentId()
        );
        System.out.println("THIS IS THE UPDATED INTEGER  :"+updated);
        if (updated == 0) {
            log.warn("shipment.created ignored (already processed or missing order) orderId={}",
                    event.orderId());
            return;
        }

        log.info("Processed shipment.created for orderId={} shipmentId={}",
                event.orderId(), event.shipmentId());
    }

    // =========================================================
    // shipment.status-changed
    // =========================================================

    private void handleShipmentStatusChanged(Message message) throws Exception {

        ShipmentStatusChangedEvent event =
                MAPPER.readValue(message.getBody(), ShipmentStatusChangedEvent.class);

        log.info("Consuming shipment.status-changed for orderId={} status={}",
                event.orderId(), event.newStatus());

        Order order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null) {
            log.warn("Order not found for shipment.status-changed orderId={}",
                    event.orderId());
            return;
        }

        order.getMetadata().put("latestShipmentStatus", event.newStatus());
        order.getMetadata().put("latestShipmentId", event.shipmentId());

        orderRepository.save(order);

        log.info("Processed shipment.status-changed for orderId={} status={}",
                event.orderId(), event.newStatus());
    }
}